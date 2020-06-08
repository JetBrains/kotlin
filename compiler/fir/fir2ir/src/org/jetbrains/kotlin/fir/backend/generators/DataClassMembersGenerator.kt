/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.declareThisReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBooleanTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitIntTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNullableAnyTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitStringTypeRef
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrGeneratorContextBase
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.DataClassMembersGenerator
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

/**
 * A generator that generates synthetic members of data class as well as part of inline class.
 *
 * This one uses [DataClassMembersGenerator] to generate function bodies, shared with the counterpart in psi. But, there are two main
 * differences. Unlike the counterpart in psi, which uses descriptor-based logic to determine which members to synthesize, this one uses
 * fir own logic that traverses class hierarchies in fir elements. Also, this one creates and passes IR elements, instead of providing how
 * to declare them, to [DataClassMembersGenerator].
 */
@OptIn(DescriptorBasedIr::class)
class DataClassMembersGenerator(val components: Fir2IrComponents) {

    fun generateInlineClassMembers(klass: FirClass<*>, irClass: IrClass): List<Name> =
        MyDataClassMethodsGenerator(irClass, klass.symbol.classId, IrDeclarationOrigin.GENERATED_INLINE_CLASS_MEMBER).generate(klass)

    fun generateDataClassMembers(klass: FirClass<*>, irClass: IrClass): List<Name> =
        MyDataClassMethodsGenerator(irClass, klass.symbol.classId, IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER).generate(klass)

    fun generateDataClassComponentBody(irFunction: IrFunction, classId: ClassId) =
        MyDataClassMethodsGenerator(irFunction.parentAsClass, classId, IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER)
            .generateComponentBody(irFunction)

    fun generateDataClassCopyBody(irFunction: IrFunction, classId: ClassId) =
        MyDataClassMethodsGenerator(irFunction.parentAsClass, classId, IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER)
            .generateCopyBody(irFunction)

    private inner class MyDataClassMethodsGenerator(val irClass: IrClass, val classId: ClassId, val origin: IrDeclarationOrigin) {
        private val irDataClassMembersGenerator = object : DataClassMembersGenerator(
            IrGeneratorContextBase(components.irBuiltIns),
            components.symbolTable,
            irClass,
            origin
        ) {
            override fun declareSimpleFunction(startOffset: Int, endOffset: Int, functionDescriptor: FunctionDescriptor): IrFunction {
                throw IllegalStateException("Not expect to see function declaration.")
            }

            override fun generateSyntheticFunctionParameterDeclarations(irFunction: IrFunction) {
                // TODO
            }

            override fun getBackingField(parameter: ValueParameterDescriptor?, irValueParameter: IrValueParameter?): IrField? =
                irValueParameter?.let {
                    irClass.properties.single { irProperty ->
                        irProperty.name == irValueParameter.name && irProperty.backingField?.type == irValueParameter.type
                    }.backingField
                }

            override fun transform(typeParameterDescriptor: TypeParameterDescriptor): IrType {
                // TODO
                return components.irBuiltIns.anyType
            }

            override fun commitSubstituted(irMemberAccessExpression: IrMemberAccessExpression, descriptor: CallableDescriptor) {
                // TODO
            }
        }

        fun generateDispatchReceiverParameter(irFunction: IrFunction, valueParameterDescriptor: WrappedValueParameterDescriptor) =
            irFunction.declareThisReceiverParameter(
                components.symbolTable,
                irClass.defaultType,
                origin,
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET
            ).apply {
                valueParameterDescriptor.bind(this)
            }


        private val FirSimpleFunction.matchesEqualsSignature: Boolean
            get() = valueParameters.size == 1 &&
                    valueParameters[0].returnTypeRef.toIrType(components.typeConverter) == components.irBuiltIns.anyNType &&
                    returnTypeRef.toIrType(components.typeConverter) == components.irBuiltIns.booleanType

        private val FirSimpleFunction.matchesHashCodeSignature: Boolean
            get() = valueParameters.isEmpty() &&
                    returnTypeRef.toIrType(components.typeConverter) == components.irBuiltIns.intType

        private val FirSimpleFunction.matchesToStringSignature: Boolean
            get() = valueParameters.isEmpty() &&
                    returnTypeRef.toIrType(components.typeConverter) == components.irBuiltIns.stringType

        private val FirSimpleFunction.matchesDataClassSyntheticMemberSignatures: Boolean
            get() = (this.name == equalsName && matchesEqualsSignature) ||
                    (this.name == hashCodeName && matchesHashCodeSignature) ||
                    (this.name == toStringName && matchesToStringSignature)

        fun generate(klass: FirClass<*>): List<Name> {
            val propertyParametersCount = irClass.primaryConstructor?.explicitParameters?.size ?: 0
            val properties = irClass.declarations
                .filterIsInstance<IrProperty>()
                .take(propertyParametersCount)
                .map { it.descriptor }
            if (properties.isEmpty()) {
                return emptyList()
            }

            val result = mutableListOf<Name>()

            val contributedFunctionsInThisType = klass.declarations.mapNotNull {
                if (it is FirSimpleFunction && it.matchesDataClassSyntheticMemberSignatures) {
                    it.name
                } else
                    null
            }
            val nonOverridableContributedFunctionsInSupertypes =
                klass.collectContributedFunctionsFromSupertypes(components.session) { declaration, map ->
                    if (declaration is FirSimpleFunction &&
                        declaration.body != null &&
                        !Visibilities.isPrivate(declaration.visibility) &&
                        declaration.modality == Modality.FINAL &&
                        declaration.matchesDataClassSyntheticMemberSignatures
                    ) {
                        map.putIfAbsent(declaration.name, declaration)
                    }
                }

            if (!contributedFunctionsInThisType.contains(equalsName) &&
                !nonOverridableContributedFunctionsInSupertypes.containsKey(equalsName)
            ) {
                result.add(equalsName)
                val equalsFunction = createSyntheticIrFunction(
                    equalsName,
                    components.irBuiltIns.booleanType,
                ) { createSyntheticIrParameter(it, Name.identifier("other"), components.irBuiltIns.anyNType) }
                irDataClassMembersGenerator.generateEqualsMethod(equalsFunction, properties)
                irClass.declarations.add(equalsFunction)
            }

            if (!contributedFunctionsInThisType.contains(hashCodeName) &&
                !nonOverridableContributedFunctionsInSupertypes.containsKey(hashCodeName)
            ) {
                result.add(hashCodeName)
                val hashCodeFunction = createSyntheticIrFunction(
                    hashCodeName,
                    components.irBuiltIns.intType,
                )
                irDataClassMembersGenerator.generateHashCodeMethod(hashCodeFunction, properties)
                irClass.declarations.add(hashCodeFunction)
            }

            if (!contributedFunctionsInThisType.contains(toStringName) &&
                !nonOverridableContributedFunctionsInSupertypes.containsKey(toStringName)
            ) {
                result.add(toStringName)
                val toStringFunction = createSyntheticIrFunction(
                    toStringName,
                    components.irBuiltIns.stringType,
                )
                irDataClassMembersGenerator.generateToStringMethod(toStringFunction, properties)
                irClass.declarations.add(toStringFunction)
            }

            return result
        }

        fun generateComponentBody(irFunction: IrFunction) {
            val index = getComponentIndex(irFunction)!!
            val valueParameter = irClass.primaryConstructor!!.valueParameters[index - 1]
            val backingField = irDataClassMembersGenerator.getBackingField(null, valueParameter)!!
            irDataClassMembersGenerator
                .generateComponentFunction(irFunction, backingField, valueParameter.startOffset, valueParameter.endOffset)
        }

        fun generateCopyBody(irFunction: IrFunction) =
            irDataClassMembersGenerator.generateCopyFunction(irFunction, irClass.primaryConstructor!!.symbol)

        private fun createSyntheticIrFunction(
            name: Name,
            returnType: IrType,
            valueParameterBuilder: (IrFunction) -> IrValueParameter? = { null }
        ): IrFunction {
            val functionDescriptor = WrappedSimpleFunctionDescriptor()
            val thisReceiverDescriptor = WrappedValueParameterDescriptor()
            return components.symbolTable.declareSimpleFunction(functionDescriptor) { symbol ->
                IrFunctionImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    origin,
                    symbol,
                    name,
                    Visibilities.PUBLIC,
                    Modality.OPEN,
                    returnType,
                    isInline = false,
                    isExternal = false,
                    isTailrec = false,
                    isSuspend = false,
                    isExpect = false,
                    isFakeOverride = false,
                    isOperator = false
                ).apply {
                    val irValueParameter = valueParameterBuilder(this)?.let {
                        this.valueParameters = listOf(it)
                        it
                    }
                    metadata = FirMetadataSource.Function(
                        buildSimpleFunction {
                            origin = FirDeclarationOrigin.Synthetic
                            this.name = name
                            this.symbol = FirNamedFunctionSymbol(CallableId(classId.packageFqName, classId.relativeClassName, name))
                            this.status = FirDeclarationStatusImpl(Visibilities.PUBLIC, Modality.FINAL)
                            this.session = components.session
                            this.returnTypeRef = when (returnType) {
                                components.irBuiltIns.booleanType -> FirImplicitBooleanTypeRef(null)
                                components.irBuiltIns.intType -> FirImplicitIntTypeRef(null)
                                components.irBuiltIns.stringType -> FirImplicitStringTypeRef(null)
                                else -> throw AssertionError("Should not be here")
                            }
                            if (irValueParameter != null) {
                                this.valueParameters.add(
                                    buildValueParameter {
                                        this.name = irValueParameter.name
                                        origin = FirDeclarationOrigin.Synthetic
                                        this.session = components.session
                                        this.returnTypeRef = FirImplicitNullableAnyTypeRef(null)
                                        this.symbol = FirVariableSymbol(irValueParameter.name)
                                        isCrossinline = false
                                        isNoinline = false
                                        isVararg = false
                                    }
                                )
                            }
                        }
                    )

                }
            }.apply {
                parent = irClass
                functionDescriptor.bind(this)
                dispatchReceiverParameter = generateDispatchReceiverParameter(this, thisReceiverDescriptor)
                components.irBuiltIns.anyClass.descriptor.unsubstitutedMemberScope
                    .getContributedFunctions(this.name, NoLookupLocation.FROM_BACKEND)
                    .singleOrNull { function -> function.name == this.name }
                    ?.let {
                        overriddenSymbols = listOf(components.symbolTable.referenceSimpleFunction(it))
                    }
            }
        }

        private fun createSyntheticIrParameter(irFunction: IrFunction, name: Name, type: IrType, index: Int = 0): IrValueParameter {
            val descriptor = WrappedValueParameterDescriptor()
            return components.symbolTable.declareValueParameter(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                origin,
                descriptor,
                type
            ) { symbol ->
                IrValueParameterImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    origin,
                    symbol,
                    name,
                    index,
                    type,
                    null,
                    isCrossinline = false,
                    isNoinline = false
                )
            }.apply {
                parent = irFunction
                descriptor.bind(this)
            }
        }
    }

    companion object {
        private val copyName = Name.identifier("copy")
        private val equalsName = Name.identifier("equals")
        private val hashCodeName = Name.identifier("hashCode")
        private val toStringName = Name.identifier("toString")

        fun isCopy(irFunction: IrFunction): Boolean =
            irFunction.name == copyName

        fun isComponentN(irFunction: IrFunction): Boolean {
            if (irFunction.name.isSpecial) {
                return false
            }
            val name = irFunction.name.identifier
            if (!name.startsWith("component")) {
                return false
            }
            val n = getComponentIndex(irFunction)
            return n != null && n > 0
        }

        fun getComponentIndex(irFunction: IrFunction): Int? =
            irFunction.name.identifier.substring("component".length).toIntOrNull()
    }
}