/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.builtins.StandardNames.HASHCODE_NAME
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.backend.declareThisReceiverParameter
import org.jetbrains.kotlin.fir.backend.toIrType
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBooleanTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitIntTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNullableAnyTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitStringTypeRef
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrGeneratorContextBase
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.DataClassMembersGenerator
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DataClassResolver
import org.jetbrains.kotlin.util.OperatorNameConventions.EQUALS
import org.jetbrains.kotlin.util.OperatorNameConventions.TO_STRING

/**
 * A generator that generates synthetic members of data class as well as part of inline class.
 *
 * This one uses [DataClassMembersGenerator] to generate function bodies, shared with the counterpart in psi. But, there are two main
 * differences. Unlike the counterpart in psi, which uses descriptor-based logic to determine which members to synthesize, this one uses
 * fir own logic that traverses class hierarchies in fir elements. Also, this one creates and passes IR elements, instead of providing how
 * to declare them, to [DataClassMembersGenerator].
 */
class DataClassMembersGenerator(val components: Fir2IrComponents) : Fir2IrComponents by components {

    fun generateSingleFieldValueClassMembers(klass: FirClass, irClass: IrClass): List<FirDeclaration> =
        MyDataClassMethodsGenerator(irClass, klass.symbol.toLookupTag(), IrDeclarationOrigin.GENERATED_SINGLE_FIELD_VALUE_CLASS_MEMBER)
            .generate(klass)

    fun generateMultiFieldValueClassMembers(klass: FirClass, irClass: IrClass): List<FirDeclaration> =
        MyDataClassMethodsGenerator(irClass, klass.symbol.toLookupTag(), IrDeclarationOrigin.GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER)
            .generate(klass)

    fun generateDataClassMembers(klass: FirClass, irClass: IrClass): List<FirDeclaration> =
        MyDataClassMethodsGenerator(irClass, klass.symbol.toLookupTag(), IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER).generate(klass)

    fun generateDataClassComponentBody(irFunction: IrFunction, lookupTag: ConeClassLikeLookupTag) =
        MyDataClassMethodsGenerator(irFunction.parentAsClass, lookupTag, IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER)
            .generateComponentBody(irFunction)

    fun generateDataClassCopyBody(irFunction: IrFunction, lookupTag: ConeClassLikeLookupTag) =
        MyDataClassMethodsGenerator(irFunction.parentAsClass, lookupTag, IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER)
            .generateCopyBody(irFunction)

    private inner class MyDataClassMethodsGenerator(
        val irClass: IrClass,
        val lookupTag: ConeClassLikeLookupTag,
        val origin: IrDeclarationOrigin
    ) {
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

            override fun getProperty(parameter: ValueParameterDescriptor?, irValueParameter: IrValueParameter?): IrProperty? =
                irValueParameter?.let {
                    irClass.properties.single { irProperty ->
                        irProperty.name == irValueParameter.name && irProperty.backingField?.type == irValueParameter.type
                    }
                }

            override fun transform(typeParameterDescriptor: TypeParameterDescriptor): IrType {
                // TODO
                return components.irBuiltIns.anyType
            }

            inner class Fir2IrHashCodeFunctionInfo(override val symbol: IrSimpleFunctionSymbol) : HashCodeFunctionInfo {
                override fun commitSubstituted(irMemberAccessExpression: IrMemberAccessExpression<*>) {
                    // TODO
                }
            }

            private fun getHashCodeFunction(klass: IrClass): IrSimpleFunctionSymbol =
                klass.functions.singleOrNull {
                    it.name.asString() == "hashCode" && it.valueParameters.isEmpty() && it.extensionReceiverParameter == null
                }?.symbol
                    ?: context.irBuiltIns.anyClass.functions.single { it.owner.name.asString() == "hashCode" }


            val IrTypeParameter.erasedUpperBound: IrClass
                get() {
                    // Pick the (necessarily unique) non-interface upper bound if it exists
                    for (type in superTypes) {
                        val irClass = type.classOrNull?.owner ?: continue
                        if (!irClass.isInterface && !irClass.isAnnotationClass) return irClass
                    }

                    // Otherwise, choose either the first IrClass supertype or recurse.
                    // In the first case, all supertypes are interface types and the choice was arbitrary.
                    // In the second case, there is only a single supertype.
                    return when (val firstSuper = superTypes.first().classifierOrNull?.owner) {
                        is IrClass -> firstSuper
                        is IrTypeParameter -> firstSuper.erasedUpperBound
                        else -> error("unknown supertype kind $firstSuper")
                    }
                }


            override fun getHashCodeFunctionInfo(type: IrType): HashCodeFunctionInfo {
                val classifier = type.classifierOrNull
                val symbol = when {
                    classifier.isArrayOrPrimitiveArray -> context.irBuiltIns.dataClassArrayMemberHashCodeSymbol
                    classifier is IrClassSymbol -> getHashCodeFunction(classifier.owner)
                    classifier is IrTypeParameterSymbol -> getHashCodeFunction(classifier.owner.erasedUpperBound)
                    else -> error("Unknown classifier kind $classifier")
                }
                return Fir2IrHashCodeFunctionInfo(symbol)
            }
        }

        fun generateDispatchReceiverParameter(irFunction: IrFunction) =
            irFunction.declareThisReceiverParameter(
                irClass.defaultType,
                origin,
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET
            )


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
            get() = (this.name == EQUALS && matchesEqualsSignature) ||
                    (this.name == HASHCODE_NAME && matchesHashCodeSignature) ||
                    (this.name == TO_STRING && matchesToStringSignature)

        fun generate(klass: FirClass): List<FirDeclaration> {
            val propertyParametersCount = irClass.primaryConstructor?.explicitParameters?.size ?: 0
            val properties = irClass.properties.filter { it.backingField != null }.take(propertyParametersCount).toList()
            if (properties.isEmpty()) {
                return emptyList()
            }

            val result = mutableListOf<FirDeclaration>()

            val contributedFunctionsInThisType = klass.declarations.mapNotNull {
                if (it is FirSimpleFunction && it.matchesDataClassSyntheticMemberSignatures) {
                    it.name
                } else
                    null
            }
            val scope = klass.unsubstitutedScope(
                components.session,
                components.scopeSession,
                withForcedTypeCalculator = true
            )
            val contributedFunctionsInSupertypes =
                @OptIn(ExperimentalStdlibApi::class)
                buildMap<Name, FirSimpleFunction> {
                    for (name in listOf(EQUALS, HASHCODE_NAME, TO_STRING)) {
                        // We won't synthesize a function if there is a user-contributed one.
                        if (contributedFunctionsInThisType.contains(name)) continue
                        scope.processFunctionsByName(name) {
                            val declaration = it.fir
                            if (declaration.matchesDataClassSyntheticMemberSignatures && declaration.modality != Modality.FINAL) {
                                putIfAbsent(declaration.name, declaration)
                            }
                        }
                    }
                }

            val equalsContributedFunction = contributedFunctionsInSupertypes[EQUALS]
            if (equalsContributedFunction != null) {
                result.add(equalsContributedFunction)
                val equalsFunction = createSyntheticIrFunction(
                    EQUALS,
                    components.irBuiltIns.booleanType,
                    otherParameterNeeded = true
                )
                irDataClassMembersGenerator.generateEqualsMethod(equalsFunction, properties)
                irClass.declarations.add(equalsFunction)
            }

            val hashcodeNameContributedFunction = contributedFunctionsInSupertypes[HASHCODE_NAME]
            if (hashcodeNameContributedFunction != null) {
                result.add(hashcodeNameContributedFunction)
                val hashCodeFunction = createSyntheticIrFunction(
                    HASHCODE_NAME,
                    components.irBuiltIns.intType,
                )
                irDataClassMembersGenerator.generateHashCodeMethod(hashCodeFunction, properties)
                irClass.declarations.add(hashCodeFunction)
            }

            val toStringContributedFunction = contributedFunctionsInSupertypes[TO_STRING]
            if (toStringContributedFunction != null) {
                result.add(toStringContributedFunction)
                val toStringFunction = createSyntheticIrFunction(
                    TO_STRING,
                    components.irBuiltIns.stringType,
                )
                irDataClassMembersGenerator.generateToStringMethod(toStringFunction, properties)
                irClass.declarations.add(toStringFunction)
            }

            return result
        }

        fun generateComponentBody(irFunction: IrFunction) {
            val index = DataClassResolver.getComponentIndex(irFunction.name.asString())
            val valueParameter = irClass.primaryConstructor!!.valueParameters[index - 1]
            val irProperty = irDataClassMembersGenerator.getProperty(null, valueParameter)!!
            irDataClassMembersGenerator.generateComponentFunction(irFunction, irProperty)
        }

        fun generateCopyBody(irFunction: IrFunction) =
            irDataClassMembersGenerator.generateCopyFunction(irFunction, irClass.primaryConstructor!!.symbol)

        private fun createSyntheticIrFunction(
            name: Name,
            returnType: IrType,
            otherParameterNeeded: Boolean = false
        ): IrFunction {
            val firFunction = buildSimpleFunction {
                origin = FirDeclarationOrigin.Synthetic
                this.name = name
                this.symbol = FirNamedFunctionSymbol(CallableId(lookupTag.classId, name))
                this.status = FirDeclarationStatusImpl(Visibilities.Public, Modality.FINAL)
                moduleData = components.session.moduleData
                this.returnTypeRef = when (returnType) {
                    components.irBuiltIns.booleanType -> FirImplicitBooleanTypeRef(null)
                    components.irBuiltIns.intType -> FirImplicitIntTypeRef(null)
                    components.irBuiltIns.stringType -> FirImplicitStringTypeRef(null)
                    else -> error("Unexpected synthetic data class function return type: $returnType")
                }
                if (otherParameterNeeded) {
                    this.valueParameters.add(
                        buildValueParameter {
                            this.name = Name.identifier("other")
                            origin = FirDeclarationOrigin.Synthetic
                            moduleData = components.session.moduleData
                            this.returnTypeRef = FirImplicitNullableAnyTypeRef(null)
                            this.symbol = FirValueParameterSymbol(this.name)
                            isCrossinline = false
                            isNoinline = false
                            isVararg = false
                        }
                    )
                }
                dispatchReceiverType = lookupTag.constructType(
                    (1..irClass.typeParameters.size).map { ConeStarProjection }.toTypedArray(), isNullable = false
                )
            }
            val signature = if (lookupTag.classId.isLocal) null else components.signatureComposer.composeSignature(firFunction)
            return components.declarationStorage.declareIrSimpleFunction(signature, null) { symbol ->
                components.irFactory.createFunction(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, symbol, name, DescriptorVisibilities.PUBLIC, Modality.OPEN, returnType,
                    isInline = false, isExternal = false, isTailrec = false, isSuspend = false, isOperator = false,
                    isInfix = false, isExpect = false, isFakeOverride = false,
                ).apply {
                    if (otherParameterNeeded) {
                        val irValueParameter = createSyntheticIrParameter(
                            this, firFunction.valueParameters.first().name, components.irBuiltIns.anyNType
                        )
                        this.valueParameters = listOf(irValueParameter)
                    }
                    metadata = FirMetadataSource.Function(
                        firFunction
                    )
                }
            }.apply {
                parent = irClass
                dispatchReceiverParameter = generateDispatchReceiverParameter(this)
                components.irBuiltIns.findBuiltInClassMemberFunctions(
                    components.irBuiltIns.anyClass,
                    this.name
                ).singleOrNull()?.let {
                    overriddenSymbols = listOf(it)
                }
            }
        }

        private fun createSyntheticIrParameter(irFunction: IrFunction, name: Name, type: IrType, index: Int = 0): IrValueParameter =
            components.irFactory.createValueParameter(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, IrValueParameterSymbolImpl(), name, index, type, null,
                isCrossinline = false, isNoinline = false, isHidden = false, isAssignable = false
            ).apply {
                parent = irFunction
            }
    }
}
