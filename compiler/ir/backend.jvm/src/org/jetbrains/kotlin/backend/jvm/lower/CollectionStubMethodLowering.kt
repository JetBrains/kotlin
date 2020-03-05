/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*

internal val collectionStubMethodLowering = makeIrFilePhase(
    ::CollectionStubMethodLowering,
    name = "CollectionStubMethod",
    description = "Generate Collection stub methods"
)

private class CollectionStubMethodLowering(val context: JvmBackendContext) : ClassLoweringPass {
    private val methodSignatureMapper = context.methodSignatureMapper

    override fun lower(irClass: IrClass) {
        if (irClass.isInterface) {
            return
        }

        val methodStubsToGenerate = generateRelevantStubMethods(irClass)
        if (methodStubsToGenerate.isEmpty()) return

        // We don't need to generate stub for existing methods, but for FAKE_OVERRIDE methods with ABSTRACT modality,
        // it means an abstract function in superclass that is not implemented yet,
        // stub generation is still needed to avoid invocation error.
        val existingMethodSignatures = irClass.functions.filterNot {
            it.modality == Modality.ABSTRACT && it.origin == IrDeclarationOrigin.FAKE_OVERRIDE
        }.associateBy { it.toSignature() }

        for (member in methodStubsToGenerate) {
            val existingMethod = existingMethodSignatures[member.toSignature()]
            if (existingMethod != null) {
                // In the case that we find a defined method that matches the stub signature, we add the overridden symbols to that
                // defined method, so that bridge lowering can still generate correct bridge for that method
                existingMethod.overriddenSymbols += member.overriddenSymbols
            } else {
                irClass.declarations.add(member)
            }
        }
    }

    private fun IrSimpleFunction.toSignature(): String = methodSignatureMapper.mapAsmMethod(this).toString()

    private fun createStubMethod(
        function: IrSimpleFunction, irClass: IrClass, substitutionMap: Map<IrTypeParameterSymbol, IrType>
    ): IrSimpleFunction {
        return buildFun {
            name = function.name
            returnType = function.returnType.substitute(substitutionMap)
            visibility = function.visibility
            origin = IrDeclarationOrigin.IR_BUILTINS_STUB
            modality = Modality.OPEN
        }.apply {
            // Replace Function metadata with the data from class
            // Add the abstract function symbol to stub function for bridge lowering
            overriddenSymbols = listOf(function.symbol)
            parent = irClass
            dispatchReceiverParameter = function.dispatchReceiverParameter?.copyWithSubstitution(this, substitutionMap)
            extensionReceiverParameter = function.extensionReceiverParameter?.copyWithSubstitution(this, substitutionMap)
            valueParameters = function.valueParameters.map { it.copyWithSubstitution(this, substitutionMap) }
            // Function body consist only of throwing UnsupportedOperationException statement
            body = context.createIrBuilder(function.symbol).irBlockBody {
                +irThrow(
                    irCall(
                        this@CollectionStubMethodLowering.context.ir.symbols.ThrowUnsupportOperationExceptionClass
                    ).apply {
                        putValueArgument(0, irString("Operation is not supported for read-only collection"))
                    }
                )
            }
        }
    }

    // Copy value parameter with type substitution
    private fun IrValueParameter.copyWithSubstitution(target: IrSimpleFunction, substitutionMap: Map<IrTypeParameterSymbol, IrType>)
            : IrValueParameter {
        val descriptor = WrappedValueParameterDescriptor(this.descriptor.annotations)
        return IrValueParameterImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            IrDeclarationOrigin.IR_BUILTINS_STUB,
            IrValueParameterSymbolImpl(descriptor),
            name,
            index,
            type.substitute(substitutionMap),
            varargElementType?.substitute(substitutionMap),
            isCrossinline,
            isNoinline
        ).apply {
            descriptor.bind(this)
            parent = target
        }
    }

    // Compute a substitution map for type parameters between source class (Mutable Collection classes) to
    // target class (class currently in lowering phase), this map is later used for substituting type parameters in generated functions
    private fun computeSubstitutionMap(readOnlyClass: IrClass, mutableClass: IrClass, targetClass: IrClass)
            : Map<IrTypeParameterSymbol, IrType> {
        // We find the most specific type for the immutable collection class from the inheritance chain of target class
        // Perform type substitution along searching, then use the type arguments obtained from the most specific type
        // for type substitution.
        val readOnlyClassType = getAllSupertypes(targetClass).findMostSpecificTypeForClass(readOnlyClass.symbol)
        val readOnlyClassTypeArguments = (readOnlyClassType as IrSimpleType).arguments.mapNotNull { (it as? IrTypeProjection)?.type }

        if (readOnlyClassTypeArguments.isEmpty() || readOnlyClassTypeArguments.size != mutableClass.typeParameters.size) {
            throw IllegalStateException(
                "Type argument mismatch between immutable class ${readOnlyClass.fqNameWhenAvailable}" +
                        " and mutable class ${mutableClass.fqNameWhenAvailable}, when processing" +
                        "class ${targetClass.fqNameWhenAvailable}"
            )
        }
        return mutableClass.typeParameters.map { it.symbol }.zip(readOnlyClassTypeArguments).toMap()
    }

    private inner class StubsForCollectionClass(
        val readOnlyClass: IrClassSymbol,
        val mutableClass: IrClassSymbol
    ) {
        val mutableOnlyMethods: Collection<IrSimpleFunction> by lazy {
            val readOnlyMethodSignatures = readOnlyClass.functions.map { it.owner.toSignature() }.toHashSet()
            mutableClass.functions
                .map { it.owner }
                .filter { it.toSignature() !in readOnlyMethodSignatures }
                .toHashSet()
        }

        operator fun component1() = readOnlyClass
        operator fun component2() = mutableClass
        operator fun component3() = mutableOnlyMethods
    }

    private val preComputedStubs: Collection<StubsForCollectionClass> by lazy {
        with(context.ir.symbols) {
            listOf(
                collection to mutableCollection,
                set to mutableSet,
                list to mutableList,
                map to mutableMap,
                mapEntry to mutableMapEntry,
                iterable to mutableIterable,
                iterator to mutableIterator,
                listIterator to mutableListIterator
            ).map { (readOnlyClass, mutableClass) ->
                StubsForCollectionClass(readOnlyClass, mutableClass)
            }
        }
    }

    // Compute stubs that should be generated, compare based on signature
    private fun generateRelevantStubMethods(irClass: IrClass): Set<IrSimpleFunction> {
        val ourStubsForCollectionClasses = preComputedStubs.filter { (readOnlyClass, mutableClass) ->
            irClass.superTypes.any { supertypeSymbol ->
                val supertype = supertypeSymbol.classOrNull?.owner
                // We need to generate stub methods for following 2 cases:
                // current class's direct super type is a java class or kotlin interface, and is an subtype of an immutable collection
                supertype != null
                        && (supertype.comesFromJava() || supertype.isInterface)
                        && supertypeSymbol.isSubtypeOfClass(readOnlyClass)
                        && !irClass.symbol.isSubtypeOfClass(mutableClass)
            }
        }

        // do a second filtering to ensure only most relevant classes are included.
        val redundantClasses = ourStubsForCollectionClasses.filter { (readOnlyClass) ->
            ourStubsForCollectionClasses.any { readOnlyClass != it.readOnlyClass && it.readOnlyClass.isSubtypeOfClass(readOnlyClass) }
        }.map { it.readOnlyClass }

        // perform type substitution and type erasure here
        return ourStubsForCollectionClasses.filter { (readOnlyClass) ->
            readOnlyClass !in redundantClasses
        }.flatMap { (readOnlyClass, mutableClass, mutableOnlyMethods) ->
            val substitutionMap = computeSubstitutionMap(readOnlyClass.owner, mutableClass.owner, irClass)
            mutableOnlyMethods.map { function ->
                createStubMethod(function, irClass, substitutionMap)
            }
        }.toHashSet()
    }

    fun IrClass.comesFromJava() = origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB

    private fun Collection<IrType>.findMostSpecificTypeForClass(classifier: IrClassSymbol): IrType {
        val types = this.filter { it.classifierOrNull == classifier }
        if (types.isEmpty()) error("No supertype of $classifier in $this")
        if (types.size == 1) return types.first()
        // Find the first type in the list such that it's a subtype of every other type in that list
        return types.first { type ->
            types.all { other -> type.isSubtypeOfClass(other.classOrNull!!) }
        }
    }
}