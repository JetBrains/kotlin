/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.utils.defaultTypeWithoutArguments
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.BuiltInOperatorNames
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.ArrayFqNames
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

@OptIn(Fir2IrBuiltInsInternals::class)
class IrBuiltInsOverFir(
    private val c: Fir2IrComponents,
    private val moduleDescriptor: FirModuleDescriptor,
    private val syntheticSymbolsContainer: Fir2IrSyntheticIrBuiltinsSymbolsContainer
) : IrBuiltIns() {

    // ------------------------------------- basic stuff -------------------------------------

    private val session: FirSession
        get() = c.session

    private val symbolProvider: FirSymbolProvider
        get() = session.symbolProvider

    override val languageVersionSettings: LanguageVersionSettings
        get() = session.languageVersionSettings

    override val irFactory: IrFactory = c.irFactory

    private val kotlinPackage: FqName = StandardClassIds.BASE_KOTLIN_PACKAGE

    private val fir2irBuiltins = c.builtins

    override val kotlinInternalPackageFragment: IrExternalPackageFragment = createPackage(StandardClassIds.BASE_INTERNAL_PACKAGE)
    private val kotlinInternalIrPackageFragment: IrExternalPackageFragment = createPackage(StandardClassIds.BASE_INTERNAL_IR_PACKAGE)
    override val operatorsPackageFragment: IrExternalPackageFragment
        get() = kotlinInternalIrPackageFragment

    // ------------------------------------- normal classes and functions -------------------------------------

    override val booleanNotSymbol: IrSimpleFunctionSymbol get() = fir2irBuiltins.booleanNotSymbol

    override val anyClass: IrClassSymbol get() = fir2irBuiltins.anyClass

    override val anyType: IrType get() = fir2irBuiltins.anyType
    override val anyNType: IrType get() = fir2irBuiltins.anyNType

    override val numberClass: IrClassSymbol get() = fir2irBuiltins.numberClass
    override val numberType: IrType get() = numberClass.defaultTypeWithoutArguments

    override val nothingClass: IrClassSymbol get() = fir2irBuiltins.nothingClass
    override val nothingType: IrType get() = fir2irBuiltins.nothingType
    override val nothingNType: IrType get() = fir2irBuiltins.nothingNType

    override val unitClass: IrClassSymbol get() = fir2irBuiltins.unitClass
    override val unitType: IrType get() = fir2irBuiltins.unitType

    override val booleanClass: IrClassSymbol get() = fir2irBuiltins.booleanClass
    override val booleanType: IrType get() = fir2irBuiltins.booleanType

    override val charClass: IrClassSymbol get() = fir2irBuiltins.charClass
    override val charType: IrType get() = fir2irBuiltins.charType

    override val byteClass: IrClassSymbol get() = fir2irBuiltins.byteClass
    override val byteType: IrType get() = fir2irBuiltins.byteType

    override val shortClass: IrClassSymbol get() = fir2irBuiltins.shortClass
    override val shortType: IrType get() = fir2irBuiltins.shortType

    override val intClass: IrClassSymbol get() = fir2irBuiltins.intClass
    override val intType: IrType get() = fir2irBuiltins.intType

    override val longClass: IrClassSymbol get() = fir2irBuiltins.longClass
    override val longType: IrType get() = fir2irBuiltins.longType

    override val floatClass: IrClassSymbol get() = fir2irBuiltins.floatClass
    override val floatType: IrType get() = fir2irBuiltins.floatType

    override val doubleClass: IrClassSymbol get() = fir2irBuiltins.doubleClass
    override val doubleType: IrType get() = fir2irBuiltins.doubleType

    override val charSequenceClass: IrClassSymbol get() = fir2irBuiltins.charSequenceClass

    override val stringClass: IrClassSymbol get() = fir2irBuiltins.stringClass
    override val stringType: IrType get() = fir2irBuiltins.stringType

    override val iteratorClass: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.Iterator) }
    override val arrayClass: IrClassSymbol get() = fir2irBuiltins.arrayClass

    override val annotationClass: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.Annotation) }
    override val annotationType: IrType get() = annotationClass.defaultTypeWithoutArguments

    override val collectionClass: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.Collection) }
    override val setClass: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.Set) }
    override val listClass: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.List) }
    override val mapClass: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.Map) }
    override val mapEntryClass: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.MapEntry) }

    override val iterableClass: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.Iterable) }
    override val listIteratorClass: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.ListIterator) }
    override val mutableCollectionClass: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.MutableCollection) }
    override val mutableSetClass: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.MutableSet) }
    override val mutableListClass: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.MutableList) }
    override val mutableMapClass: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.MutableMap) }
    override val mutableMapEntryClass: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.MutableMapEntry) }

    override val mutableIterableClass: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.MutableIterable) }
    override val mutableIteratorClass: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.MutableIterator) }
    override val mutableListIteratorClass: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.MutableListIterator) }
    override val comparableClass: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.Comparable) }
    override val throwableType: IrType by lazy { throwableClass.defaultTypeWithoutArguments }
    override val throwableClass: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.Throwable) }

    override val kCallableClass: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.KCallable) }
    override val kPropertyClass: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.KProperty) }
    override val kClassClass: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.KClass) }
    override val kTypeClass: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.KType) }
    override val kProperty0Class: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.KProperty0) }
    override val kProperty1Class: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.KProperty1) }
    override val kProperty2Class: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.KProperty2) }
    override val kMutableProperty0Class: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.KMutableProperty0) }
    override val kMutableProperty1Class: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.KMutableProperty1) }
    override val kMutableProperty2Class: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.KMutableProperty2) }

    override val functionClass: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.Function) }
    override val kFunctionClass: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.KFunction) }

    override val primitiveTypeToIrType: Map<PrimitiveType, IrType> get() = fir2irBuiltins.primitiveTypeToIrType
    private val primitiveIntegralIrTypes: List<IrType> by lazy { listOf(byteType, shortType, intType, longType) }
    override val primitiveFloatingPointIrTypes: List<IrType> by lazy { listOf(floatType, doubleType) }
    private val primitiveNumericIrTypes: List<IrType> by lazy { primitiveIntegralIrTypes + primitiveFloatingPointIrTypes }
    override val primitiveIrTypesWithComparisons: List<IrType> by lazy { listOf(charType) + primitiveNumericIrTypes }
    override val primitiveIrTypes: List<IrType> by lazy { listOf(booleanType) + primitiveIrTypesWithComparisons }
    private val baseIrTypes: List<IrType> by lazy { primitiveIrTypes + stringType }

    private fun primitiveIterator(primitiveType: PrimitiveType): IrClassSymbol {
        val classId = ClassId(StandardClassIds.BASE_COLLECTIONS_PACKAGE, Name.identifier("${primitiveType.typeName}Iterator"))
        return fir2irBuiltins.loadClass(classId)
    }

    override val booleanIterator: IrClassSymbol by lazy { primitiveIterator(PrimitiveType.BOOLEAN) }
    override val charIterator: IrClassSymbol by lazy { primitiveIterator(PrimitiveType.CHAR) }
    override val byteIterator: IrClassSymbol by lazy { primitiveIterator(PrimitiveType.BYTE) }
    override val shortIterator: IrClassSymbol by lazy { primitiveIterator(PrimitiveType.SHORT) }
    override val intIterator: IrClassSymbol by lazy { primitiveIterator(PrimitiveType.INT) }
    override val longIterator: IrClassSymbol by lazy { primitiveIterator(PrimitiveType.LONG) }
    override val floatIterator: IrClassSymbol by lazy { primitiveIterator(PrimitiveType.FLOAT) }
    override val doubleIterator: IrClassSymbol by lazy { primitiveIterator(PrimitiveType.DOUBLE) }

    override val booleanArray: IrClassSymbol get() = fir2irBuiltins.booleanArray
    override val charArray: IrClassSymbol get() = fir2irBuiltins.charArray
    override val byteArray: IrClassSymbol get() = fir2irBuiltins.byteArray
    override val shortArray: IrClassSymbol get() = fir2irBuiltins.shortArray
    override val intArray: IrClassSymbol get() = fir2irBuiltins.intArray
    override val longArray: IrClassSymbol get() = fir2irBuiltins.longArray
    override val floatArray: IrClassSymbol get() = fir2irBuiltins.floatArray
    override val doubleArray: IrClassSymbol get() = fir2irBuiltins.doubleArray

    override val primitiveArraysToPrimitiveTypes: Map<IrClassSymbol, PrimitiveType> get() = fir2irBuiltins.primitiveArraysToPrimitiveTypes
    override val primitiveTypesToPrimitiveArrays: Map<PrimitiveType, IrClassSymbol>
        get() = primitiveArraysToPrimitiveTypes.map { (k, v) -> v to k }.toMap()

    override val primitiveArrayElementTypes: Map<IrClassSymbol, IrType?> get() = fir2irBuiltins.primitiveArrayElementTypes
    override val primitiveArrayForType: Map<IrType?, IrClassSymbol> get() = fir2irBuiltins.primitiveArrayForType

    override val arrayOfNulls: IrSimpleFunctionSymbol by lazy {
        val firSymbol = symbolProvider
            .getTopLevelFunctionSymbols(kotlinPackage, ArrayFqNames.ARRAY_OF_NULLS_FUNCTION).first {
                it.fir.valueParameters.singleOrNull()?.returnTypeRef?.coneType?.isInt == true
            }
        fir2irBuiltins.findFunction(firSymbol)
    }

    override val unsignedTypesToUnsignedArrays: Map<UnsignedType, IrClassSymbol> by lazy {
        UnsignedType.entries.mapNotNull { unsignedType ->
            val array = fir2irBuiltins.loadClassSafe(unsignedType.arrayClassId)
            if (array == null) null else unsignedType to array
        }.toMap()
    }

    override val unsignedArraysElementTypes: Map<IrClassSymbol, IrType?> get() = fir2irBuiltins.unsignedArraysElementTypes

    override fun getKPropertyClass(mutable: Boolean, n: Int): IrClassSymbol = when (n) {
        0 -> if (mutable) kMutableProperty0Class else kProperty0Class
        1 -> if (mutable) kMutableProperty1Class else kProperty1Class
        2 -> if (mutable) kMutableProperty2Class else kProperty2Class
        else -> error("No KProperty for n=$n mutable=$mutable")
    }

    override val enumClass: IrClassSymbol by lazy { fir2irBuiltins.loadClass(StandardClassIds.Enum) }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override val intPlusSymbol: IrSimpleFunctionSymbol
        get() = intClass.functions.single {
            it.owner.name == OperatorNameConventions.PLUS && it.owner.valueParameters[0].type == intType
        }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override val intTimesSymbol: IrSimpleFunctionSymbol
        get() = intClass.functions.single {
            it.owner.name == OperatorNameConventions.TIMES && it.owner.valueParameters[0].type == intType
        }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override val intXorSymbol: IrSimpleFunctionSymbol
        get() = intClass.functions.single {
            it.owner.name == OperatorNameConventions.XOR && it.owner.valueParameters[0].type == intType
        }

    override val extensionToString: IrSimpleFunctionSymbol by lazy {
        val firFunctionSymbol = symbolProvider.getTopLevelFunctionSymbols(kotlinPackage, OperatorNameConventions.TO_STRING).single {
            it.receiverParameter?.typeRef?.coneType?.isNullableAny == true
        }
        fir2irBuiltins.findFunction(firFunctionSymbol)
    }

    override val memberToString: IrSimpleFunctionSymbol by lazy {
        val firFunction = fir2irBuiltins.findFirMemberFunctions(StandardClassIds.Any, OperatorNameConventions.TO_STRING).single {
            it.fir.valueParameters.isEmpty()
        }
        fir2irBuiltins.findFunction(firFunction)
    }

    override val extensionStringPlus: IrSimpleFunctionSymbol by lazy {
        val firFunction = symbolProvider.getTopLevelFunctionSymbols(kotlinPackage, OperatorNameConventions.PLUS).single { symbol ->
            val isStringExtension = symbol.fir.receiverParameter?.typeRef?.coneType?.isNullableString == true
            isStringExtension && symbol.fir.valueParameters.singleOrNull { it.returnTypeRef.coneType.isNullableAny } != null
        }
        fir2irBuiltins.findFunction(firFunction)
    }

    override val memberStringPlus: IrSimpleFunctionSymbol by lazy {
        val firFunction = fir2irBuiltins.findFirMemberFunctions(StandardClassIds.String, OperatorNameConventions.PLUS).single {
            it.fir.valueParameters.singleOrNull()?.returnTypeRef?.coneType?.isNullableAny == true
        }
        fir2irBuiltins.findFunction(firFunction)
    }

    override val arrayOf: IrSimpleFunctionSymbol by lazy {
        // distinct() is needed because we can get two Fir symbols for arrayOf function (from builtins and from stdlib)
        //   with the same IR symbol for them
        fir2irBuiltins.findFunctions(kotlinPackage, ArrayFqNames.ARRAY_OF_FUNCTION).distinct().single()
    }

    // ------------------------------------- function types -------------------------------------

    override fun getNonBuiltInFunctionsByExtensionReceiver(
        name: Name,
        vararg packageNameSegments: String,
    ): Map<IrClassifierSymbol, IrSimpleFunctionSymbol> {
        return fir2irBuiltins.getFunctionsByKey(
            name,
            *packageNameSegments,
            mapKey = { symbol ->
                symbol.fir.receiverParameter?.typeRef?.toIrType(c)?.classifierOrNull
            },
            mapValue = { _, irSymbol -> irSymbol }
        )
    }

    override fun getNonBuiltinFunctionsByReturnType(
        name: Name,
        vararg packageNameSegments: String,
    ): Map<IrClassifierSymbol, IrSimpleFunctionSymbol> {
        return fir2irBuiltins.getFunctionsByKey(
            name,
            *packageNameSegments,
            mapKey = { it.fir.returnTypeRef.toIrType(c).classifierOrNull },
            mapValue = { _, irSymbol -> irSymbol }
        )
    }

    private val functionNMap: MutableMap<Int, IrClass> = mutableMapOf()
    private val kFunctionNMap: MutableMap<Int, IrClass> = mutableMapOf()
    private val suspendFunctionNMap: MutableMap<Int, IrClass> = mutableMapOf()
    private val kSuspendFunctionNMap: MutableMap<Int, IrClass> = mutableMapOf()

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun functionN(arity: Int): IrClass = functionNMap.getOrPut(arity) {
        fir2irBuiltins.loadClass(StandardClassIds.FunctionN(arity)).owner
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun kFunctionN(arity: Int): IrClass = kFunctionNMap.getOrPut(arity) {
        fir2irBuiltins.loadClass(StandardClassIds.KFunctionN(arity)).owner
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun suspendFunctionN(arity: Int): IrClass = suspendFunctionNMap.getOrPut(arity) {
        fir2irBuiltins.loadClass(StandardClassIds.SuspendFunctionN(arity)).owner
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun kSuspendFunctionN(arity: Int): IrClass = kSuspendFunctionNMap.getOrPut(arity) {
        fir2irBuiltins.loadClass(StandardClassIds.KSuspendFunctionN(arity)).owner
    }

    // ------------------------------------- intrinsic const evaluation -------------------------------------

    private val intrinsicConstAnnotation: IrConstructorCall by lazy {
        /*
         * Old versions of stdlib may not contain @IntrinsicConstEvaluation (AV < 1.7), so in this case we should create annotation class manually
         *
         * Ideally, we should try to load it from FIR at first, but the thing is that this annotation is used for some generated builtin functions
         *   (see init section below), so if Fir2IrLazyClass for this annotation is created, it will call for `components.fakeOverrideGenerator`,
         *   which is not initialized by this moment
         * As a possible way to fix it we can move `init` section of builtins into the separate function for late initialization and call
         *   for it after Fir2IrComponentsStorage is fully initialized
         */
        val firClassSymbol = session.symbolProvider.getRegularClassSymbolByClassId(StandardClassIds.Annotations.IntrinsicConstEvaluation)

        val classSymbol = if (firClassSymbol == null) {
            val irClass = createIntrinsicConstEvaluationClass()
            irClass.symbol
        } else {
            fir2irBuiltins.loadClass(StandardClassIds.Annotations.IntrinsicConstEvaluation)
        }

        @OptIn(UnsafeDuringIrConstructionAPI::class)
        val constructor = classSymbol.owner.constructors.single()
        val constructorSymbol = constructor.symbol

        val annotationCall = IrConstructorCallImpl(
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
            valueArgumentsCount = 0,
        )

        annotationCall
    }

    // ------------------------------------- synthetics -------------------------------------

    override val ieee754equalsFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> =
        syntheticSymbolsContainer.primitiveFloatingPointTypes.associate { primitiveType ->
            val fpType = primitiveTypeToIrType.getValue(primitiveType)
            val primitiveClass = fpType.classifierOrFail
            val operator = addBuiltinOperatorSymbol(
                BuiltInOperatorNames.IEEE754_EQUALS,
                symbol = syntheticSymbolsContainer.ieee754equalsFunByOperandType.getValue(primitiveType),
                booleanType,
                "arg0" to fpType.makeNullable(),
                "arg1" to fpType.makeNullable(),
                isIntrinsicConst = true
            )
            primitiveClass to operator
        }

    override val eqeqeqSymbol: IrSimpleFunctionSymbol = addBuiltinOperatorSymbol(
        BuiltInOperatorNames.EQEQEQ,
        symbol = syntheticSymbolsContainer.eqeqeqSymbol,
        booleanType,
        "" to anyNType, "" to anyNType
    )

    override val eqeqSymbol: IrSimpleFunctionSymbol = addBuiltinOperatorSymbol(
        BuiltInOperatorNames.EQEQ,
        symbol = syntheticSymbolsContainer.eqeqSymbol,
        booleanType, "" to anyNType, "" to anyNType, isIntrinsicConst = true
    )

    override val throwCceSymbol: IrSimpleFunctionSymbol = addBuiltinOperatorSymbol(
        BuiltInOperatorNames.THROW_CCE,
        symbol = null,
        nothingType
    )

    override val throwIseSymbol: IrSimpleFunctionSymbol = addBuiltinOperatorSymbol(
        BuiltInOperatorNames.THROW_ISE,
        symbol = null,
        nothingType
    )

    override val andandSymbol: IrSimpleFunctionSymbol = addBuiltinOperatorSymbol(
        BuiltInOperatorNames.ANDAND,
        symbol = null,
        booleanType,
        "" to booleanType,
        "" to booleanType,
        isIntrinsicConst = true
    )

    override val ororSymbol: IrSimpleFunctionSymbol = addBuiltinOperatorSymbol(
        BuiltInOperatorNames.OROR,
        symbol = null,
        booleanType,
        "" to booleanType,
        "" to booleanType,
        isIntrinsicConst = true
    )

    override val noWhenBranchMatchedExceptionSymbol: IrSimpleFunctionSymbol = addBuiltinOperatorSymbol(
        BuiltInOperatorNames.NO_WHEN_BRANCH_MATCHED_EXCEPTION,
        symbol = syntheticSymbolsContainer.noWhenBranchMatchedExceptionSymbol,
        nothingType
    )

    override val illegalArgumentExceptionSymbol: IrSimpleFunctionSymbol = addBuiltinOperatorSymbol(
        BuiltInOperatorNames.ILLEGAL_ARGUMENT_EXCEPTION,
        symbol = null,
        nothingType,
        "" to stringType
    )

    override val dataClassArrayMemberHashCodeSymbol: IrSimpleFunctionSymbol = addBuiltinOperatorSymbol(
        "dataClassArrayMemberHashCode",
        symbol = null,
        intType,
        "" to anyType
    )


    override val dataClassArrayMemberToStringSymbol: IrSimpleFunctionSymbol = addBuiltinOperatorSymbol(
        "dataClassArrayMemberToString",
        symbol = null,
        stringType,
        "" to anyNType
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

    override val linkageErrorSymbol: IrSimpleFunctionSymbol
        get() = shouldNotBeCalled()

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

    // ------------------------------------- functions -------------------------------------

    override fun findFunctions(name: Name, vararg packageNameSegments: String): Iterable<IrSimpleFunctionSymbol> {
        return fir2irBuiltins.findFunctions(FqName.fromSegments(packageNameSegments.asList()), name)
    }

    override fun findFunctions(name: Name, packageFqName: FqName): Iterable<IrSimpleFunctionSymbol> {
        return fir2irBuiltins.findFunctions(packageFqName, name)
    }

    override fun findProperties(name: Name, packageFqName: FqName): Iterable<IrPropertySymbol> {
        return fir2irBuiltins.findProperties(packageFqName, name)
    }

    override fun findClass(name: Name, vararg packageNameSegments: String): IrClassSymbol? {
        return loadClassSafe(FqName.fromSegments(packageNameSegments.asList()), name)
    }

    override fun findClass(name: Name, packageFqName: FqName): IrClassSymbol? {
        return loadClassSafe(packageFqName, name)
    }

    private fun loadClassSafe(packageName: FqName, identifier: Name): IrClassSymbol? {
        return fir2irBuiltins.loadClassSafe(ClassId(packageName, identifier))
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun findBuiltInClassMemberFunctions(builtInClass: IrClassSymbol, name: Name): Iterable<IrSimpleFunctionSymbol> {
        return builtInClass.functions.filter { it.owner.name == name }.asIterable()
    }

    // This function should not be called from fir2ir code
    @UnsafeDuringIrConstructionAPI
    override fun getBinaryOperator(name: Name, lhsType: IrType, rhsType: IrType): IrSimpleFunctionSymbol {
        val definingClass = lhsType.getMaybeBuiltinClass() ?: error("Defining class not found: $lhsType")
        return definingClass.functions.single { function ->
            function.name == name && function.valueParameters.size == 1 && function.valueParameters[0].type == rhsType
        }.symbol
    }

    // This function should not be called from fir2ir code
    @UnsafeDuringIrConstructionAPI
    override fun getUnaryOperator(name: Name, receiverType: IrType): IrSimpleFunctionSymbol {
        val definingClass = receiverType.getMaybeBuiltinClass() ?: error("Defining class not found: $receiverType")
        return definingClass.functions.single { function ->
            function.name == name && function.valueParameters.isEmpty()
        }.symbol
    }

    // ------------------------------------- private utilities -------------------------------------

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrType.getMaybeBuiltinClass(): IrClass? {
        val lhsClassFqName = classFqName!!
        return baseIrTypes.find { it.classFqName == lhsClassFqName }?.getClass()
            ?: fir2irBuiltins.loadClassSafe(ClassId.topLevel(lhsClassFqName))?.owner
    }

    private fun createPackage(fqName: FqName): IrExternalPackageFragment =
        createEmptyExternalPackageFragment(moduleDescriptor, fqName)

    private fun createFunction(
        name: String,
        symbol: IrSimpleFunctionSymbol?,
        returnType: IrType,
        valueParameterTypes: Array<out Pair<String, IrType>>,
        typeParameters: List<IrTypeParameter>,
        origin: IrDeclarationOrigin,
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
            fn.parent = kotlinInternalIrPackageFragment
            // `kotlinInternalIrPackageFragment` definitely is not a lazy class
            @OptIn(UnsafeDuringIrConstructionAPI::class)
            kotlinInternalIrPackageFragment.declarations.add(fn)
        }.symbol
    }

    private fun addBuiltinOperatorSymbol(
        name: String,
        symbol: IrSimpleFunctionSymbol?,
        returnType: IrType,
        vararg valueParameterTypes: Pair<String, IrType>,
        isIntrinsicConst: Boolean = false,
    ): IrSimpleFunctionSymbol {
        return createFunction(
            name = name,
            symbol = symbol,
            returnType = returnType,
            valueParameterTypes = valueParameterTypes,
            typeParameters = emptyList(),
            origin = BUILTIN_OPERATOR,
            isIntrinsicConst = isIntrinsicConst
        )
    }

    private fun List<PrimitiveType>.defineComparisonOperatorForEachIrType(
        name: String,
        symbols: Map<PrimitiveType, IrSimpleFunctionSymbol>
    ): Map<IrClassifierSymbol, IrSimpleFunctionSymbol> {
        return associate { primitiveType ->
            val irType = primitiveTypeToIrType.getValue(primitiveType)
            irType.classifierOrFail to addBuiltinOperatorSymbol(
                name,
                symbol = symbols.getValue(primitiveType),
                booleanType,
                "" to irType,
                "" to irType,
                isIntrinsicConst = true
            )
        }
    }
}
