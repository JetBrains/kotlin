/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.builtins.StandardNames.DATA_CLASS_COPY
import org.jetbrains.kotlin.builtins.StandardNames.HASHCODE_NAME
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.backend.Fir2IrCommonMemberStorage
import org.jetbrains.kotlin.fir.backend.Fir2IrCommonMemberStorage.DataValueClassGeneratedMembersInfo
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.backend.utils.declareThisReceiverParameter
import org.jetbrains.kotlin.fir.backend.utils.declaredScope
import org.jetbrains.kotlin.fir.backend.utils.unsubstitutedScope
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.fromPrimaryConstructor
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrGeneratorContextBase
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.Companion.GENERATED_DATA_CLASS_MEMBER
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.Companion.GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.Companion.GENERATED_SINGLE_FIELD_VALUE_CLASS_MEMBER
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.DataClassResolver
import org.jetbrains.kotlin.util.OperatorNameConventions.EQUALS
import org.jetbrains.kotlin.util.OperatorNameConventions.TO_STRING
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

/**
 * A generator that generates synthetic members of data class as well as part of inline class.
 *
 * This one uses [DataClassMembersGenerator] to generate function bodies, shared with the counterpart in psi. But, there are two main
 * differences. Unlike the counterpart in psi, which uses descriptor-based logic to determine which members to synthesize, this one uses
 * fir own logic that traverses class hierarchies in fir elements. Also, this one creates and passes IR elements, instead of providing how
 * to declare them, to [DataClassMembersGenerator].
 */
class Fir2IrDataClassMembersGenerator(
    private val c: Fir2IrComponents,
    private val commonMemberStorage: Fir2IrCommonMemberStorage,
) : Fir2IrComponents by c {
    fun generateSingleFieldValueClassMembers(klass: FirRegularClass, irClass: IrClass): List<FirDeclaration> {
        return MyDataClassMethodsGenerator(irClass, klass, GENERATED_SINGLE_FIELD_VALUE_CLASS_MEMBER).generateHeaders()
    }

    fun generateMultiFieldValueClassMembers(klass: FirRegularClass, irClass: IrClass): List<FirDeclaration> {
        return MyDataClassMethodsGenerator(irClass, klass, GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER).generateHeaders()
    }

    fun generateDataClassMembers(klass: FirRegularClass, irClass: IrClass): List<FirDeclaration> {
        return MyDataClassMethodsGenerator(irClass, klass, GENERATED_DATA_CLASS_MEMBER).generateHeaders()
    }

    fun registerCopyOrComponentFunction(irFunction: IrSimpleFunction) {
        commonMemberStorage.generatedDataValueClassSyntheticFunctions.getValue(irFunction.parentAsClass).generatedFunctions += irFunction
    }

    private inner class MyDataClassMethodsGenerator(val irClass: IrClass, val klass: FirRegularClass, val origin: IrDeclarationOrigin) {
        fun generateDispatchReceiverParameter(irFunction: IrFunction): IrValueParameter =
            irFunction.declareThisReceiverParameter(
                c,
                irClass.defaultType,
                IrDeclarationOrigin.DEFINED,
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET
            )

        fun generateHeaders(): List<FirDeclaration> {
            val result = mutableListOf<FirDeclaration>()
            val contributedSyntheticFunctions = calculateSyntheticFirFunctions()

            val generatedIrFunctions = mutableListOf<IrSimpleFunction>()

            val toStringContributedFunction = contributedSyntheticFunctions[TO_STRING]
            if (toStringContributedFunction != null) {
                result.add(toStringContributedFunction)
                val toStringFunction = createSyntheticIrFunctionFromAny(
                    TO_STRING,
                    toStringContributedFunction,
                    c.builtins.stringType,
                )
                declarationStorage.cacheGeneratedFunction(toStringContributedFunction, toStringFunction)
                generatedIrFunctions += toStringFunction
            }

            val hashcodeNameContributedFunction = contributedSyntheticFunctions[HASHCODE_NAME]
            if (hashcodeNameContributedFunction != null) {
                result.add(hashcodeNameContributedFunction)
                val hashCodeFunction = createSyntheticIrFunctionFromAny(
                    HASHCODE_NAME,
                    hashcodeNameContributedFunction,
                    c.builtins.intType,
                )
                declarationStorage.cacheGeneratedFunction(hashcodeNameContributedFunction, hashCodeFunction)
                generatedIrFunctions += hashCodeFunction
            }

            val equalsContributedFunction = contributedSyntheticFunctions[EQUALS]
            if (equalsContributedFunction != null) {
                result.add(equalsContributedFunction)
                val equalsFunction = createSyntheticIrFunctionFromAny(
                    EQUALS,
                    equalsContributedFunction,
                    c.builtins.booleanType,
                    otherParameterNeeded = true,
                    isOperator = true
                )
                declarationStorage.cacheGeneratedFunction(equalsContributedFunction, equalsFunction)
                generatedIrFunctions += equalsFunction
            }

            commonMemberStorage.generatedDataValueClassSyntheticFunctions[irClass] = DataValueClassGeneratedMembersInfo(
                c, klass, origin, generatedIrFunctions
            )

            return result
        }

        private fun calculateSyntheticFirFunctions(): Map<Name, FirSimpleFunction> {
            val scope = klass.unsubstitutedScope(c)
            val contributedSyntheticFunctions =
                buildMap<Name, FirSimpleFunction> {
                    for (name in listOf(EQUALS, HASHCODE_NAME, TO_STRING)) {
                        scope.processFunctionsByName(name) {
                            // We won't synthesize a function if there is a user-contributed (non-synthetic) one.
                            if (it.origin !is FirDeclarationOrigin.Synthetic) return@processFunctionsByName
                            if (it.containingClassLookupTag() != klass.symbol.toLookupTag()) return@processFunctionsByName
                            require(!contains(name)) {
                                "Two synthetic functions $name were found in data/value class ${klass.name}:\n" +
                                        "${this[name]?.render()}\n${it.fir.render()}"
                            }
                            this[name] = it.fir
                        }
                    }
                }
            return contributedSyntheticFunctions
        }

        private fun createSyntheticIrFunctionFromAny(
            name: Name,
            syntheticCounterpart: FirSimpleFunction,
            returnType: IrType,
            otherParameterNeeded: Boolean = false,
            isOperator: Boolean = false,
        ): IrSimpleFunction {
            val symbol = c.declarationStorage.createFunctionSymbol()
            return c.irFactory.createSimpleFunction(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                origin = origin,
                name = name,
                visibility = DescriptorVisibilities.PUBLIC,
                isInline = false,
                isExpect = false,
                returnType = returnType,
                modality = Modality.OPEN,
                symbol = symbol,
                isTailrec = false,
                isSuspend = false,
                isOperator = isOperator,
                isInfix = false,
                isExternal = false,
                isFakeOverride = false,
            ).apply {
                if (otherParameterNeeded) {
                    val irValueParameter = createSyntheticIrParameter(
                        this, syntheticCounterpart.valueParameters.first().name, c.builtins.anyNType
                    )
                    this.valueParameters = listOf(irValueParameter)
                }
                metadata = FirMetadataSource.Function(syntheticCounterpart)
                setParent(irClass)
                addDeclarationToParent(this, irClass)
                dispatchReceiverParameter = generateDispatchReceiverParameter(this)
            }
        }

        private fun createSyntheticIrParameter(irFunction: IrFunction, name: Name, type: IrType, index: Int = 0): IrValueParameter =
            c.irFactory.createValueParameter(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                origin = IrDeclarationOrigin.DEFINED,
                name = name,
                type = type,
                isAssignable = false,
                symbol = IrValueParameterSymbolImpl(),
                index = index,
                varargElementType = null,
                isCrossinline = false,
                isNoinline = false,
                isHidden = false
            ).apply {
                parent = irFunction
            }
    }
}

class Fir2IrDataClassGeneratedMemberBodyGenerator(private val irBuiltins: IrBuiltIns) {
    fun generateBodiesForClassesWithSyntheticDataClassMembers(
        members: Map<IrClass, DataValueClassGeneratedMembersInfo>,
        symbolTable: SymbolTable,
    ) {
        for ((irClass, info) in members) {
            val (c, firClass, origin, functions) = info
            MyDataClassMethodsGenerator(c, irClass, firClass, origin, symbolTable).generateBodies(functions)
        }
    }

    private inner class MyDataClassMethodsGenerator(
        val c: Fir2IrComponents,
        val irClass: IrClass,
        val klass: FirRegularClass,
        val origin: IrDeclarationOrigin,
        symbolTable: SymbolTable,
    ) : Fir2IrComponents by c {
        // `irClass` is a source class and definitely is not a lazy class
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        fun generateBodies(functions: List<IrSimpleFunction>) {
            val propertyParametersCount = irClass.primaryConstructor?.explicitParameters?.size ?: 0
            val properties = irClass.properties.filter { it.backingField != null }.take(propertyParametersCount).toList()

            for (irFunction in functions) {
                when (val name = irFunction.name) {
                    TO_STRING -> irDataClassMembersGenerator.generateToStringMethod(irFunction, properties)
                    HASHCODE_NAME -> irDataClassMembersGenerator.generateHashCodeMethod(irFunction, properties)
                    EQUALS -> irDataClassMembersGenerator.generateEqualsMethod(irFunction, properties)
                    DATA_CLASS_COPY -> {
                        irFunction.origin = GENERATED_DATA_CLASS_MEMBER
                        irDataClassMembersGenerator.generateCopyFunction(irFunction, irClass.primaryConstructor!!.symbol)
                    }
                    else -> {
                        require(DataClassResolver.isComponentLike(name)) { "Unknown data class member: $name" }
                        irFunction.origin = GENERATED_DATA_CLASS_MEMBER
                        val index = DataClassResolver.getComponentIndex(irFunction.name.asString())
                        val valueParameter = irClass.primaryConstructor!!.valueParameters[index - 1]
                        val irProperty = irDataClassMembersGenerator.getProperty(valueParameter)
                        irDataClassMembersGenerator.generateComponentFunction(irFunction, irProperty)
                    }
                }
            }
        }

        /**
         * Convert types which do not have members - kotlin.Nothing and `dynamic` - to kotlin.Any.
         */
        private fun ConeKotlinType.coerceToAny(): ConeKotlinType {
            return when {
                this.isNothingOrNullableNothing -> session.builtinTypes.anyType.type
                this is ConeDynamicType -> session.builtinTypes.anyType.type
                else -> this
            }
        }

        private val irDataClassMembersGenerator = object : IrBasedDataClassMembersGenerator(
            IrGeneratorContextBase(irBuiltins),
            symbolTable,
            irClass,
            irClass.kotlinFqName,
            origin,
            forbidDirectFieldAccess = false
        ) {
            override fun generateSyntheticFunctionParameterDeclarations(irFunction: IrFunction) {
                // TODO
            }

            override fun getProperty(irValueParameter: IrValueParameter?): IrProperty {
                requireNotNull(irValueParameter)
                // `irClass` is a source class and definitely is not a lazy class
                @OptIn(UnsafeDuringIrConstructionAPI::class)
                return irClass.properties.single { irProperty ->
                    irProperty.name == irValueParameter.name && irProperty.backingField?.type == irValueParameter.type
                }
            }

            inner class Fir2IrHashCodeFunctionInfo(
                override val symbol: IrSimpleFunctionSymbol,
                override val hasDispatchReceiver: Boolean,
            ) : HashCodeFunctionInfo {
                override fun commitSubstituted(irMemberAccessExpression: IrMemberAccessExpression<*>) {
                    // TODO
                }
            }

            private fun getHashCodeFunction(klass: FirRegularClass): FirNamedFunctionSymbol {
                if (klass.classId == StandardClassIds.Nothing) {
                    // scope of kotlin.Nothing is empty, so we need to search for `hashCode` in the scope of kotlin.Any
                    return getHashCodeFunction(session.builtinTypes.anyType.type.toRegularClassSymbol(session)!!.fir)
                }
                val scope = klass.symbol.unsubstitutedScope(c)
                return scope.getFunctions(HASHCODE_NAME).first { symbol ->
                    val function = symbol.fir
                    function.valueParameters.isEmpty() && function.receiverParameter == null && function.contextReceivers.isEmpty()
                }
            }

            @Suppress("RecursivePropertyAccessor")
            val FirTypeParameter.erasedUpperBound: FirRegularClass
                get() {
                    // Pick the (necessarily unique) non-interface upper bound if it exists
                    for (type in bounds) {
                        val klass = type.coneType.coerceToAny().toRegularClassSymbol(session)?.fir ?: continue
                        val kind = klass.classKind
                        if (kind != ClassKind.INTERFACE && kind != ClassKind.ANNOTATION_CLASS) return klass
                    }

                    // Otherwise, choose either the first IrClass supertype or recurse.
                    // In the first case, all supertypes are interface types and the choice was arbitrary.
                    // In the second case, there is only a single supertype.
                    val firstBoundType = bounds.first().coneType.fullyExpandedType(session).coerceToAny()
                    return when (val firstSuper = firstBoundType.toSymbol(session)?.fir) {
                        is FirRegularClass -> firstSuper
                        is FirTypeParameter -> firstSuper.erasedUpperBound
                        else -> error("unknown supertype kind $firstSuper")
                    }
                }

            override fun getHashCodeFunctionInfo(type: IrType): HashCodeFunctionInfo {
                shouldNotBeCalled()
            }

            override fun getHashCodeFunctionInfo(property: IrProperty): HashCodeFunctionInfo {
                val firProperty = klass.symbol.declaredScope(c)
                    .getProperties(property.name)
                    .first { (it as FirPropertySymbol).fromPrimaryConstructor } as FirPropertySymbol

                val type = firProperty.resolvedReturnType.fullyExpandedType(session)
                val (symbol, hasDispatchReceiver) = when {
                    type.isArrayOrPrimitiveArray(checkUnsignedArrays = false) -> context.irBuiltIns.dataClassArrayMemberHashCodeSymbol to false
                    else -> {
                        val preparedType = type.unwrapFlexibleAndDefinitelyNotNull().coerceToAny()
                        val classForType = when (val classifier = preparedType.toSymbol(session)?.fir) {
                            is FirRegularClass -> classifier
                            is FirTypeParameter -> classifier.erasedUpperBound
                            else -> error("Unknown classifier kind $classifier")
                        }
                        val firHashCode = getHashCodeFunction(classForType)
                        val lookupTag = classForType.symbol.toLookupTag()
                        val functionSymbol = declarationStorage.getIrFunctionSymbol(firHashCode, lookupTag) as IrSimpleFunctionSymbol
                        functionSymbol to (firHashCode.dispatchReceiverType != null)
                    }
                }
                return Fir2IrHashCodeFunctionInfo(symbol, hasDispatchReceiver)
            }
        }
    }
}
