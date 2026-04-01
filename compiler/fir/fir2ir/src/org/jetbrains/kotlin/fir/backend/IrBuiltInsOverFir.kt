/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.expressions.impl.IrAnnotationImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

@OptIn(Fir2IrBuiltInsInternals::class, InternalSymbolFinderAPI::class)
class IrBuiltInsOverFir(
    private val c: Fir2IrComponents,
    private val syntheticSymbolsContainer: Fir2IrSyntheticIrBuiltinsSymbolsContainer
) : IrBuiltInsOverSymbolFinder(SymbolFinderOverFir(c.builtins)) {

    // ------------------------------------- basic stuff -------------------------------------

    private val moduleDescriptor: FirModuleDescriptor = run {
        val session = c.session
        val moduleData = when (session.languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation)) {
            false -> session.moduleData.dependencies.first()
            true -> session.moduleData
        }
        c.declarationStorage.getDependenciesModuleDescriptor(moduleData)
    }

    private val session: FirSession
        get() = c.session

    override val languageVersionSettings: LanguageVersionSettings
        get() = session.languageVersionSettings

    override val irFactory: IrFactory = IrFactoryImpl

    private val fir2irBuiltins = c.builtins

    override val kotlinInternalPackageFragment: IrExternalPackageFragment = createPackage(StandardClassIds.BASE_INTERNAL_PACKAGE)
    override val operatorsPackageFragment: IrExternalPackageFragment = createPackage(StandardClassIds.BASE_INTERNAL_IR_PACKAGE)

    // ------------------------------------- intrinsic const evaluation -------------------------------------

    private val intrinsicConstAnnotation: IrAnnotation by lazy {
        /*
         * Old versions of stdlib may not contain @IntrinsicConstEvaluation (AV < 1.7), so in this case we should create annotation class manually
         *
         * Ideally, we should try to load it from FIR at first, but the thing is that this annotation is used for some generated builtin functions
         *   (see init section below), so if Fir2IrLazyClass for this annotation is created, it will call for `components.fakeOverrideGenerator`,
         *   which is not initialized by this moment
         * As a possible way to fix it we can move `init` section of builtins into the separate function for late initialization and call
         *   for it after Fir2IrComponentsStorage is fully initialized
         */
        val firClassSymbol = session.getRegularClassSymbolByClassId(StandardClassIds.Annotations.IntrinsicConstEvaluation)

        val classSymbol = if (firClassSymbol == null) {
            val irClass = createIntrinsicConstEvaluationClass()
            irClass.symbol
        } else {
            fir2irBuiltins.loadClass(StandardClassIds.Annotations.IntrinsicConstEvaluation)
        }

        @OptIn(UnsafeDuringIrConstructionAPI::class)
        val constructor = classSymbol.owner.constructors.single()
        val constructorSymbol = constructor.symbol

        val annotationCall = IrAnnotationImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = IrSimpleTypeImpl(
                classifier = classSymbol,
                nullability = SimpleTypeNullability.DEFINITELY_NOT_NULL,
                arguments = emptyList(),
                annotations = emptyList()
            ),
            constructorSymbol,
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0,
        )

        annotationCall
    }

    // ------------------------------------- synthetics -------------------------------------

    override val ieee754equalsFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> =
        syntheticSymbolsContainer.primitiveFloatingPointTypes.associate { primitiveType ->
            val fpType = primitiveTypeToIrType.getValue(primitiveType)
            val primitiveClass = fpType.classifierOrFail
            val operator = createFunction(
                name = BuiltInOperatorNames.IEEE754_EQUALS,
                symbol = syntheticSymbolsContainer.ieee754equalsFunByOperandType.getValue(primitiveType),
                returnType = booleanType,
                valueParameterTypes = arrayOf("arg0" to fpType.makeNullable(), "arg1" to fpType.makeNullable()),
                isIntrinsicConst = true
            )
            primitiveClass to operator
        }

    override val eqeqeqSymbol: IrSimpleFunctionSymbol = createFunction(
        name = BuiltInOperatorNames.EQEQEQ,
        symbol = syntheticSymbolsContainer.eqeqeqSymbol,
        returnType = booleanType,
        valueParameterTypes = arrayOf("" to anyNType, "" to anyNType),
        isIntrinsicConst = false
    )

    override val eqeqSymbol: IrSimpleFunctionSymbol = createFunction(
        name = BuiltInOperatorNames.EQEQ,
        symbol = syntheticSymbolsContainer.eqeqSymbol,
        returnType = booleanType,
        valueParameterTypes = arrayOf("" to anyNType, "" to anyNType),
        isIntrinsicConst = true
    )

    override val throwCceSymbol: IrSimpleFunctionSymbol = createFunction(
        name = BuiltInOperatorNames.THROW_CCE,
        symbol = null,
        returnType = nothingType,
        valueParameterTypes = arrayOf<Pair<String, IrType>>(),
        isIntrinsicConst = false
    )

    override val throwIseSymbol: IrSimpleFunctionSymbol = createFunction(
        name = BuiltInOperatorNames.THROW_ISE,
        symbol = null,
        returnType = nothingType,
        valueParameterTypes = arrayOf<Pair<String, IrType>>(),
        isIntrinsicConst = false
    )

    override val andandSymbol: IrSimpleFunctionSymbol = createFunction(
        name = BuiltInOperatorNames.ANDAND,
        symbol = null,
        returnType = booleanType,
        valueParameterTypes = arrayOf("" to booleanType, "" to booleanType),
        isIntrinsicConst = true
    )

    override val ororSymbol: IrSimpleFunctionSymbol = createFunction(
        name = BuiltInOperatorNames.OROR,
        symbol = null,
        returnType = booleanType,
        valueParameterTypes = arrayOf("" to booleanType, "" to booleanType),
        isIntrinsicConst = true
    )

    override val noWhenBranchMatchedExceptionSymbol: IrSimpleFunctionSymbol = createFunction(
        name = BuiltInOperatorNames.NO_WHEN_BRANCH_MATCHED_EXCEPTION,
        symbol = syntheticSymbolsContainer.noWhenBranchMatchedExceptionSymbol,
        returnType = nothingType,
        valueParameterTypes = arrayOf<Pair<String, IrType>>(),
        isIntrinsicConst = false
    )

    override val illegalArgumentExceptionSymbol: IrSimpleFunctionSymbol = createFunction(
        name = BuiltInOperatorNames.ILLEGAL_ARGUMENT_EXCEPTION,
        symbol = null,
        returnType = nothingType,
        valueParameterTypes = arrayOf("" to stringType),
        isIntrinsicConst = false
    )

    override val dataClassArrayMemberHashCodeSymbol: IrSimpleFunctionSymbol = createFunction(
        name = "dataClassArrayMemberHashCode",
        symbol = null,
        returnType = intType,
        valueParameterTypes = arrayOf("" to anyType),
        isIntrinsicConst = false
    )


    override val dataClassArrayMemberToStringSymbol: IrSimpleFunctionSymbol = createFunction(
        name = "dataClassArrayMemberToString",
        symbol = null,
        returnType = stringType,
        valueParameterTypes = arrayOf("" to anyNType),
        isIntrinsicConst = false
    )

    override val checkNotNullSymbol: IrSimpleFunctionSymbol = run {
        val typeParameter = irFactory.createTypeParameter(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = BUILTIN_OPERATOR,
            name = Name.identifier("T0"),
            symbol = IrTypeParameterSymbolImpl(),
            variance = Variance.INVARIANT,
            index = 0,
            isReified = true
        ).apply {
            superTypes = listOf(anyType)
        }

        createFunction(
            name = BuiltInOperatorNames.CHECK_NOT_NULL,
            symbol = syntheticSymbolsContainer.checkNotNullSymbol,
            returnType = IrSimpleTypeImpl(typeParameter.symbol, SimpleTypeNullability.DEFINITELY_NOT_NULL, emptyList(), emptyList()),
            valueParameterTypes = arrayOf("" to IrSimpleTypeImpl(typeParameter.symbol, hasQuestionMark = true, emptyList(), emptyList())),
            typeParameters = listOf(typeParameter),
            origin = BUILTIN_OPERATOR,
            isIntrinsicConst = false,
        )
    }

    override val linkageErrorSymbol: IrSimpleFunctionSymbol = createFunction(
        name = "linkageErrorSymbol",
        symbol = null,
        returnType = nothingType,
        valueParameterTypes = arrayOf("" to anyNType),
        isIntrinsicConst = false
    )

    override val lessFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> =
        syntheticSymbolsContainer.primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(
            BuiltInOperatorNames.LESS,
            syntheticSymbolsContainer.lessFunByOperandType
        )

    override val lessOrEqualFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> =
        syntheticSymbolsContainer.primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(
            BuiltInOperatorNames.LESS_OR_EQUAL,
            syntheticSymbolsContainer.lessOrEqualFunByOperandType
        )

    override val greaterOrEqualFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> =
        syntheticSymbolsContainer.primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(
            BuiltInOperatorNames.GREATER_OR_EQUAL,
            syntheticSymbolsContainer.greaterOrEqualFunByOperandType
        )

    override val greaterFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> =
        syntheticSymbolsContainer.primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(
            BuiltInOperatorNames.GREATER,
            syntheticSymbolsContainer.greaterFunByOperandType
        )

    // ------------------------------------- private utilities -------------------------------------
    private fun createPackage(fqName: FqName): IrExternalPackageFragment =
        createEmptyExternalPackageFragment(moduleDescriptor, fqName)

    private fun createFunction(
        name: String,
        symbol: IrSimpleFunctionSymbol?,
        returnType: IrType,
        valueParameterTypes: Array<out Pair<String, IrType>>,
        typeParameters: List<IrTypeParameter> = emptyList(),
        origin: IrDeclarationOrigin = BUILTIN_OPERATOR,
        isIntrinsicConst: Boolean,
    ): IrSimpleFunctionSymbol {
        return irFactory.createSimpleFunction(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = origin,
            name = Name.identifier(name),
            visibility = DescriptorVisibilities.PUBLIC,
            isInline = false,
            isExpect = false,
            returnType = returnType,
            modality = Modality.FINAL,
            symbol = symbol ?: IrSimpleFunctionSymbolImpl(),
            isTailrec = false,
            isSuspend = false,
            isOperator = false,
            isInfix = false,
            isExternal = false,
            containerSource = null,
            isFakeOverride = false,
        ).also { fn ->
            valueParameterTypes.forEachIndexed { index, (pName, irType) ->
                fn.addValueParameter(Name.identifier(pName.ifBlank { "arg$index" }), irType, origin)
            }
            fn.typeParameters = typeParameters
            typeParameters.forEach { it.parent = fn }
            if (isIntrinsicConst) {
                fn.annotations += intrinsicConstAnnotation
            }
            fn.parent = operatorsPackageFragment
            // `operatorsPackageFragment` definitely is not a lazy class
            @OptIn(UnsafeDuringIrConstructionAPI::class)
            operatorsPackageFragment.declarations.add(fn)
        }.symbol
    }

    private fun List<PrimitiveType>.defineComparisonOperatorForEachIrType(
        name: String,
        symbols: Map<PrimitiveType, IrSimpleFunctionSymbol>
    ): Map<IrClassifierSymbol, IrSimpleFunctionSymbol> {
        return associate { primitiveType ->
            val irType = primitiveTypeToIrType.getValue(primitiveType)
            irType.classifierOrFail to createFunction(
                name = name,
                symbol = symbols.getValue(primitiveType),
                returnType = booleanType,
                valueParameterTypes = arrayOf("" to irType, "" to irType),
                isIntrinsicConst = true
            )
        }
    }
}

@OptIn(Fir2IrBuiltInsInternals::class, UnsafeDuringIrConstructionAPI::class)
@InternalSymbolFinderAPI
class SymbolFinderOverFir(private val fir2irBuiltins: Fir2IrBuiltinSymbolsContainer) : SymbolFinder() {
    override fun findClass(classId: ClassId): IrClassSymbol? {
        return fir2irBuiltins.loadClassSafe(classId)
    }

    override fun findFunctions(callableId: CallableId): Iterable<IrSimpleFunctionSymbol> {
        return fir2irBuiltins.findFunctions(callableId)
    }

    override fun findProperties(callableId: CallableId): Iterable<IrPropertySymbol> {
        return fir2irBuiltins.findProperties(callableId)
    }
}
