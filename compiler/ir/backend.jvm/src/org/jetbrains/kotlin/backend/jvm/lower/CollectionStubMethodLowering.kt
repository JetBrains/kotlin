/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.isJvmInterface
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext
import org.jetbrains.kotlin.utils.addToStdlib.cast

internal val collectionStubMethodLowering = makeIrFilePhase(
    ::CollectionStubMethodLowering,
    name = "CollectionStubMethod",
    description = "Generate Collection stub methods"
)

internal class CollectionStubMethodLowering(val context: JvmBackendContext) : ClassLoweringPass {
    private val collectionStubComputer = context.collectionStubComputer

    private data class NameAndArity(
        val name: Name,
        val typeParametersCount: Int,
        val valueParametersCount: Int
    )

    private val IrSimpleFunction.nameAndArity
        get() = NameAndArity(name, typeParameters.size, valueParameters.size)

    override fun lower(irClass: IrClass) {
        if (irClass.isInterface) {
            return
        }

        val methodStubsToGenerate = generateRelevantStubMethods(irClass)
        if (methodStubsToGenerate.isEmpty()) return

        // We don't need to generate stub for existing methods, but for FAKE_OVERRIDE methods with ABSTRACT modality,
        // it means an abstract function in superclass that is not implemented yet,
        // stub generation is still needed to avoid invocation error.
        val existingMethodsByNameAndArity = irClass.functions
            .filterNot { it.modality == Modality.ABSTRACT && it.isFakeOverride }
            .groupBy { it.nameAndArity }

        for (stub in methodStubsToGenerate) {
            val relevantMembers = existingMethodsByNameAndArity[stub.nameAndArity].orEmpty()
            val existingOverrides = relevantMembers.filter { isStubOverriddenByExistingFun(stub, it) }

            if (existingOverrides.isNotEmpty()) {
                // In the case that we find a defined method that matches the stub signature,
                // we add the overridden symbols to that defined method,
                // so that bridge lowering can still generate correct bridge for that method.
                existingOverrides.forEach { it.overriddenSymbols += stub.overriddenSymbols }
                continue
            }

            // Some stub members require special handling.
            // In both 'remove' and 'removeAt' cases there are no other member functions with same name in built-in mutable collection
            // classes, so it's safe to check for the member name itself.
            when (stub.name.asString()) {
                "remove" -> {
                    //  - 'remove' member functions:
                    //          kotlin.collections.MutableCollection<E>#remove(E): Boolean
                    //          kotlin.collections.MutableMap<K, V>#remove(K): V?
                    //      We've checked that corresponding 'remove(T)' member function is not present in the class.
                    //      We should add a member function that overrides, respectively:
                    //          java.util.Collection<E>#remove(Object): boolean
                    //          java.util.Map<K, V>#remove(K): V
                    //      This corresponds to replacing value parameter types with 'Any?'.
                    irClass.declarations.add(stub.apply {
                        valueParameters = valueParameters.map {
                            it.copyWithCustomTypeSubstitution(this) { context.irBuiltIns.anyNType }
                        }
                    })
                }
                "removeAt" -> {
                    //  - 'removeAt' member function:
                    //          kotlin.collections.MutableList<E>#removeAt(Int): E
                    //      We've checked that corresponding 'removeAt(Int)' member function is not present in the class
                    //      (if it IS present, special bridges for 'remove(I)' would be generated later in BridgeLowering).
                    //      We can't add 'removeAt' here, because it would be different from what old back-end generates
                    //      and can break existing Java and/or Kotlin code.
                    //      We should add a member function that overrides
                    //          java.util.List<E>#remove(int): E
                    //      and throws UnsupportedOperationException, just like any other stub.
                    //      Also, we should generate a bridge for it if required.
                    val removeIntFun = createRemoveAtStub(stub, stub.returnType, IrDeclarationOrigin.IR_BUILTINS_STUB)
                    irClass.declarations.add(removeIntFun)
                    val removeIntBridgeFun = createRemoveAtStub(stub, context.irBuiltIns.anyNType, IrDeclarationOrigin.BRIDGE)
                    if (removeIntBridgeFun.toJvmSignature() != removeIntFun.toJvmSignature()) {
                        irClass.declarations.add(removeIntBridgeFun)
                    }
                }
                else ->
                    irClass.declarations.add(stub)
            }


        }
    }

    private fun createRemoveAtStub(
        removeAtStub: IrSimpleFunction,
        stubReturnType: IrType,
        stubOrigin: IrDeclarationOrigin
    ): IrSimpleFunction {
        return context.irFactory.buildFun {
            name = Name.identifier("remove")
            returnType = stubReturnType
            visibility = removeAtStub.visibility
            origin = stubOrigin
            modality = Modality.OPEN
        }.apply {
            // NB stub method for 'remove(int)' doesn't override any built-in Kotlin declaration
            parent = removeAtStub.parent
            dispatchReceiverParameter = removeAtStub.dispatchReceiverParameter?.copyWithCustomTypeSubstitution(this) { it }
            extensionReceiverParameter = null
            valueParameters = removeAtStub.valueParameters.map { stubParameter ->
                stubParameter.copyWithCustomTypeSubstitution(this) { it }
            }
            body = createThrowingStubBody(this)
        }
    }

    private fun IrSimpleFunction.toJvmSignature(): String = collectionStubComputer.getJvmSignature(this)

    private fun createStubMethod(
        function: IrSimpleFunction,
        irClass: IrClass,
        substitutionMap: Map<IrTypeParameterSymbol, IrType>
    ): IrSimpleFunction {
        return context.irFactory.buildFun {
            name = function.name
            returnType = liftStubMethodReturnType(function).substitute(substitutionMap)
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
            body = createThrowingStubBody(this)
        }
    }

    private fun liftStubMethodReturnType(function: IrSimpleFunction) =
        when (function.name.asString()) {
            "iterator" ->
                context.ir.symbols.iterator.typeWithArguments(function.returnType.cast<IrSimpleType>().arguments)
            "listIterator" ->
                context.ir.symbols.listIterator.typeWithArguments(function.returnType.cast<IrSimpleType>().arguments)
            "subList" ->
                context.ir.symbols.list.typeWithArguments(function.returnType.cast<IrSimpleType>().arguments)
            else ->
                function.returnType
        }

    private fun createThrowingStubBody(function: IrSimpleFunction) =
        context.createIrBuilder(function.symbol).irBlockBody {
            // Function body consist only of throwing UnsupportedOperationException statement
            +irCall(this@CollectionStubMethodLowering.context.ir.symbols.throwUnsupportedOperationException)
                .apply {
                    putValueArgument(0, irString("Operation is not supported for read-only collection"))
                }
        }

    private fun isStubOverriddenByExistingFun(stubFun: IrSimpleFunction, existingFun: IrSimpleFunction): Boolean {
        // We don't add a throwing stub if it's effectively overridden by an existing function.
        // This is true if all of the following conditions are met,
        // assuming type parameter Ti of the existing function is "equal" to type parameter Si of the generated stub:
        //  - names are same;
        //  - existing function has the same number of type parameters,
        //    and upper bounds for type parameters are equivalent;
        //  - existing function has the same number of value parameters,
        //    and types for value parameters are equivalent;
        //  - return type of the existing function is a subtype of return type of the generated stub.

        if (stubFun.name != existingFun.name) return false
        if (stubFun.typeParameters.size != existingFun.typeParameters.size) return false
        if (stubFun.valueParameters.size != existingFun.valueParameters.size) return false

        val typeChecker = createTypeChecker(stubFun, existingFun)

        // Note that type parameters equivalence check doesn't really happen on collection stubs
        // (because members of Kotlin built-in collection classes don't have type parameters of their own),
        // but we keep it here for the sake of consistency.
        if (!areTypeParametersEquivalent(existingFun, stubFun, typeChecker)) return false

        if (!areValueParametersEquivalent(existingFun, stubFun, typeChecker)) return false
        if (!isReturnTypeOverrideCompliant(existingFun, stubFun, typeChecker)) return false

        return true
    }

    private fun createTypeChecker(overrideFun: IrSimpleFunction, parentFun: IrSimpleFunction): AbstractTypeCheckerContext =
        IrTypeCheckerContextWithAdditionalAxioms(context.irBuiltIns, overrideFun.typeParameters, parentFun.typeParameters)

    private fun areTypeParametersEquivalent(
        overrideFun: IrSimpleFunction,
        parentFun: IrSimpleFunction,
        typeChecker: AbstractTypeCheckerContext
    ): Boolean =
        overrideFun.typeParameters.zip(parentFun.typeParameters)
            .all { (typeParameter1, typeParameter2) ->
                typeParameter1.superTypes.zip(typeParameter2.superTypes)
                    .all { (supertype1, supertype2) ->
                        AbstractTypeChecker.equalTypes(typeChecker, supertype1, supertype2)
                    }
            }

    private fun areValueParametersEquivalent(
        overrideFun: IrSimpleFunction,
        parentFun: IrSimpleFunction,
        typeChecker: AbstractTypeCheckerContext
    ): Boolean =
        overrideFun.valueParameters.zip(parentFun.valueParameters)
            .all { (valueParameter1, valueParameter2) ->
                AbstractTypeChecker.equalTypes(typeChecker, valueParameter1.type, valueParameter2.type)
            }

    internal fun isReturnTypeOverrideCompliant(
        overrideFun: IrSimpleFunction,
        parentFun: IrSimpleFunction,
        typeChecker: AbstractTypeCheckerContext
    ): Boolean =
        AbstractTypeChecker.isSubtypeOf(typeChecker, overrideFun.returnType, parentFun.returnType)

    // Copy value parameter with type substitution
    private fun IrValueParameter.copyWithSubstitution(
        target: IrSimpleFunction,
        substitutionMap: Map<IrTypeParameterSymbol, IrType>
    ): IrValueParameter =
        copyWithCustomTypeSubstitution(target) { it.substitute(substitutionMap) }

    private fun IrValueParameter.copyWithCustomTypeSubstitution(
        target: IrSimpleFunction,
        substituteType: (IrType) -> IrType
    ): IrValueParameter {
        val parameter = this
        return buildValueParameter(target) {
            origin = IrDeclarationOrigin.IR_BUILTINS_STUB
            name = parameter.name
            index = parameter.index
            type = substituteType(parameter.type)
            varargElementType = parameter.varargElementType?.let { substituteType(it) }
            isCrossInline = parameter.isCrossinline
            isNoinline = parameter.isNoinline
        }
    }

    // Compute a substitution map for type parameters between source class (Mutable Collection classes) to
    // target class (class currently in lowering phase), this map is later used for substituting type parameters in generated functions
    private fun computeSubstitutionMap(readOnlyClass: IrClass, mutableClass: IrClass, targetClass: IrClass)
            : Map<IrTypeParameterSymbol, IrType> {
        // We find the most specific type for the immutable collection class from the inheritance chain of target class
        // Perform type substitution along searching, then use the type arguments obtained from the most specific type
        // for type substitution.
        val readOnlyClassType = getAllSubstitutedSupertypes(targetClass).findMostSpecificTypeForClass(readOnlyClass.symbol)
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

    // Compute stubs that should be generated, compare based on signature
    private fun generateRelevantStubMethods(irClass: IrClass): List<IrSimpleFunction> {
        fun createStubFuns(stubs: CollectionStubComputer.StubsForCollectionClass): List<IrSimpleFunction> {
            val (readOnlyClass, mutableClass, mutableOnlyMethods) = stubs
            val substitutionMap = computeSubstitutionMap(readOnlyClass.owner, mutableClass.owner, irClass)
            return mutableOnlyMethods.map { function ->
                createStubMethod(function, irClass, substitutionMap)
            }
        }

        val classStubs = collectionStubComputer.stubsForCollectionClasses(irClass)

        val superClassesStubs = irClass.superClass?.run {
            superClassChain.flatMap { superClass ->
                collectionStubComputer.stubsForCollectionClasses(superClass)
            }.toList()
        } ?: emptyList()

        val relevantStubs =
            classStubs.filter { (readOnlyClass) ->
                classStubs.none { readOnlyClass != it.readOnlyClass && it.readOnlyClass.isSubtypeOfClass(readOnlyClass) } &&
                        superClassesStubs.none { it.readOnlyClass == readOnlyClass }
            }

        val classStubFuns = relevantStubs.flatMap { createStubFuns(it) }
        val superClassStubSignatures = superClassesStubs.flatMap { createStubFuns(it) }.mapTo(HashSet()) { it.toJvmSignature() }

        return classStubFuns.filter { it.toJvmSignature() !in superClassStubSignatures }
    }

    private fun Collection<IrType>.findMostSpecificTypeForClass(classifier: IrClassSymbol): IrType {
        val types = this.filter { it.classifierOrNull == classifier }
        if (types.isEmpty()) error("No supertype of $classifier in $this")
        if (types.size == 1) return types.first()
        // Find the first type in the list such that it's a subtype of every other type in that list
        return types.first { type ->
            types.all { other -> type.isSubtypeOfClass(other.classOrNull!!) }
        }
    }

    private val IrClass.superClass: IrClass?
        get() = superTypes.mapNotNull { it.getClass() }.singleOrNull { !it.isJvmInterface }

    private val IrClass.superClassChain: Sequence<IrClass>
        get() = generateSequence(this) { it.superClass }
}

internal class CollectionStubComputer(val context: JvmBackendContext) {
    fun getJvmSignature(irFunction: IrSimpleFunction): String = context.methodSignatureMapper.mapAsmMethod(irFunction).toString()

    inner class StubsForCollectionClass(
        val readOnlyClass: IrClassSymbol,
        val mutableClass: IrClassSymbol
    ) {

        val mutableOnlyMethods: Collection<IrSimpleFunction> by lazy {
            val readOnlyMethodSignatures =
                readOnlyClass.functions
                    .filter { !it.owner.isSpecialCaseStubForOldBackend() }
                    .map { getJvmSignature(it.owner) }
                    .toHashSet()
            mutableClass.functions
                .map { it.owner }
                .filter { getJvmSignature(it) !in readOnlyMethodSignatures }
                .toHashSet()
        }

        operator fun component1() = readOnlyClass
        operator fun component2() = mutableClass
        operator fun component3() = mutableOnlyMethods
    }

    // Preserve old backend's logic to generate stubs for special cases where a mutable method
    // has the same JVM signature as the immutable method. See KT-36724 for more details.
    private fun IrSimpleFunction.isSpecialCaseStubForOldBackend(): Boolean {
        return when (name.asString()) {
            "iterator" -> {
                val parentClassSymbol = parentAsClass.symbol
                // Due to the specific way Kotlin built-in collection classes are written,
                // old JVM back-end generates throwing method stubs for the following abstract member functions:
                //      Iterable<T>#iterator(): Iterator<T>
                //      Collection<E>#iterator(): Iterator<E>
                //      Set<E>#iterator(): Iterator<E>
                // This happens because MutableIterable, MutableCollection, and MutableSet contain explicit override
                //      override fun iterator(): MutableIterator<E>
                // and MutableList doesn't, which makes corresponding member of MutableList a FAKE_OVERRIDE.
                with(context.ir.symbols) {
                    // Note that here we are looking at a read-only collection member.
                    parentClassSymbol == iterable || parentClassSymbol == collection || parentClassSymbol == set
                }
            }
            "listIterator", "subList" ->
                true
            else ->
                false
        }
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

    private val stubsCache = mutableMapOf<IrClass, Collection<StubsForCollectionClass>>()

    fun stubsForCollectionClasses(irClass: IrClass): Collection<StubsForCollectionClass> =
        stubsCache.getOrPut(irClass) {
            if (irClass.comesFromJava()) emptySet()
            else preComputedStubs.filter { (readOnlyClass, mutableClass) ->
                !irClass.symbol.isSubtypeOfClass(mutableClass) &&
                        irClass.superTypes.any { it.isSubtypeOfClass(readOnlyClass) }
            }
        }

    private fun IrClass.comesFromJava() = origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
}