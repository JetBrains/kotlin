/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.BuiltInOperatorNames
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.IrFunctionBuilder
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionPublicSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions

class IrBuiltInsOverFir(
    private val components: Fir2IrComponents,
    override val languageVersionSettings: LanguageVersionSettings,
    private val moduleDescriptor: FirModuleDescriptor,
    irMangler: KotlinMangler.IrMangler
) : IrBuiltIns() {
    private val session: FirSession
        get() = components.session

    private val symbolProvider: FirSymbolProvider
        get() = session.symbolProvider

    override val irFactory: IrFactory = components.irFactory

    private val kotlinPackage = StandardClassIds.BASE_KOTLIN_PACKAGE

    override val kotlinInternalPackageFragment: IrExternalPackageFragment = createPackage(StandardClassIds.BASE_INTERNAL_PACKAGE)
    private val kotlinInternalIrPackageFragment: IrExternalPackageFragment = createPackage(StandardClassIds.BASE_INTERNAL_IR_PACKAGE)
    override val operatorsPackageFragment: IrExternalPackageFragment
        get() = kotlinInternalIrPackageFragment

    private val irSignatureBuilder = PublicIdSignatureComputer(irMangler)

    override val booleanNotSymbol: IrSimpleFunctionSymbol by lazy {
        val firFunction = findFirMemberFunctions(StandardClassIds.Boolean, OperatorNameConventions.NOT)
            .first { it.resolvedReturnType.isBoolean }
        findFunction(firFunction)
    }

    private fun findFirMemberFunctions(classId: ClassId, name: Name): List<FirNamedFunctionSymbol> {
        val klass = symbolProvider.getClassLikeSymbolByClassId(classId) as FirRegularClassSymbol
        val scope = with(components) { klass.unsubstitutedScope() }
        return scope.getFunctions(name)
    }

    override val anyClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Any) }

    override val anyType: IrType get() = anyClass.defaultTypeWithoutArguments
    override val anyNType by lazy { anyType.makeNullable() }

    override val numberClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Number) }
    override val numberType: IrType get() = numberClass.defaultTypeWithoutArguments

    override val nothingClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Nothing) }
    override val nothingType: IrType get() = nothingClass.defaultTypeWithoutArguments
    override val nothingNType: IrType by lazy { nothingType.makeNullable() }

    override val unitClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Unit) }
    override val unitType: IrType get() = unitClass.defaultTypeWithoutArguments

    override val booleanClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Boolean) }
    override val booleanType: IrType get() = booleanClass.defaultTypeWithoutArguments

    override val charClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Char) }
    override val charType: IrType get() = charClass.defaultTypeWithoutArguments

    override val byteClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Byte) }
    override val byteType: IrType get() = byteClass.defaultTypeWithoutArguments

    override val shortClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Short) }
    override val shortType: IrType get() = shortClass.defaultTypeWithoutArguments

    override val intClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Int) }
    override val intType: IrType get() = intClass.defaultTypeWithoutArguments

    override val longClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Long) }
    override val longType: IrType get() = longClass.defaultTypeWithoutArguments

    override val floatClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Float) }
    override val floatType: IrType get() = floatClass.defaultTypeWithoutArguments

    override val doubleClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Double) }
    override val doubleType: IrType get() = doubleClass.defaultTypeWithoutArguments

    override val charSequenceClass: IrClassSymbol by lazy { loadClass(StandardClassIds.CharSequence) }

    override val stringClass: IrClassSymbol by lazy { loadClass(StandardClassIds.String) }
    override val stringType: IrType get() = stringClass.defaultTypeWithoutArguments

    internal val intrinsicConst by lazy {
        /*
         * Old versions of stdlib may not contain @IntrinsicConstEvaluation (AV < 1.7), so in this case we should create annotation class manually
         *
         * Ideally, we should try to load it from FIR at first, but the thing is that this annotation is used for some generated builtin functions
         *   (see init section below), so if Fir2IrLazyClass for this annotation is created, it will call for `components.fakeOverrideGenerator`,
         *   which is not initialized by this moment
         * As a possible way to fix it we can move `init` section of builtins into the separate function for late initialization and call
         *   for it after Fir2IrComponentsStorage is fully initialized
         */
        val irClass = createIntrinsicConstEvaluationClass()
        val firClassSymbol = session.symbolProvider.getClassLikeSymbolByClassId(
            StandardClassIds.Annotations.IntrinsicConstEvaluation
        ) as FirRegularClassSymbol?

        if (firClassSymbol != null) {
            /*
             * If @IntrinsicConstEvaluation is present in dependencies, we should manually cache relation between FIR and IR class
             * Without it classifier storage may create another IR class for @IntrinsicConstEvaluation, if it will be referenced
             *   somewhere in the code
             */
            @OptIn(LeakedDeclarationCaches::class)
            components.classifierStorage.cacheIrClass(firClassSymbol.fir, irClass)
        }

        irClass.symbol
    }

    private val intrinsicConstAnnotation: IrConstructorCall by lazy {
        // class for intrinsicConst is created manually and it definitely is not a lazy class
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        val constructor = intrinsicConst.constructors.single()
        IrConstructorCallImpl.Companion.fromSymbolOwner(intrinsicConst.defaultType, constructor)
    }

    override val iteratorClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Iterator) }
    override val arrayClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Array) }

    override val annotationClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Annotation) }
    override val annotationType: IrType get() = annotationClass.defaultTypeWithoutArguments

    override val collectionClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Collection) }
    override val setClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Set) }
    override val listClass: IrClassSymbol by lazy { loadClass(StandardClassIds.List) }
    override val mapClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Map) }
    override val mapEntryClass: IrClassSymbol by lazy { loadClass(StandardClassIds.MapEntry) }

    override val iterableClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Iterable) }
    override val listIteratorClass: IrClassSymbol by lazy { loadClass(StandardClassIds.ListIterator) }
    override val mutableCollectionClass: IrClassSymbol by lazy { loadClass(StandardClassIds.MutableCollection) }
    override val mutableSetClass: IrClassSymbol by lazy { loadClass(StandardClassIds.MutableSet) }
    override val mutableListClass: IrClassSymbol by lazy { loadClass(StandardClassIds.MutableList) }
    override val mutableMapClass: IrClassSymbol by lazy { loadClass(StandardClassIds.MutableMap) }
    override val mutableMapEntryClass: IrClassSymbol by lazy { loadClass(StandardClassIds.MutableMapEntry) }

    override val mutableIterableClass: IrClassSymbol by lazy { loadClass(StandardClassIds.MutableIterable) }
    override val mutableIteratorClass: IrClassSymbol by lazy { loadClass(StandardClassIds.MutableIterator) }
    override val mutableListIteratorClass: IrClassSymbol by lazy { loadClass(StandardClassIds.MutableListIterator) }
    override val comparableClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Comparable) }
    override val throwableType: IrType by lazy { throwableClass.defaultTypeWithoutArguments }
    override val throwableClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Throwable) }

    override val kCallableClass: IrClassSymbol by lazy { loadClass(StandardClassIds.KCallable) }
    override val kPropertyClass: IrClassSymbol by lazy { loadClass(StandardClassIds.KProperty) }
    override val kClassClass: IrClassSymbol by lazy { loadClass(StandardClassIds.KClass) }
    override val kTypeClass: IrClassSymbol by lazy { loadClass(StandardClassIds.KType) }
    override val kProperty0Class: IrClassSymbol by lazy { loadClass(StandardClassIds.KProperty0) }
    override val kProperty1Class: IrClassSymbol by lazy { loadClass(StandardClassIds.KProperty1) }
    override val kProperty2Class: IrClassSymbol by lazy { loadClass(StandardClassIds.KProperty2) }
    override val kMutableProperty0Class: IrClassSymbol by lazy { loadClass(StandardClassIds.KMutableProperty0) }
    override val kMutableProperty1Class: IrClassSymbol by lazy { loadClass(StandardClassIds.KMutableProperty1) }
    override val kMutableProperty2Class: IrClassSymbol by lazy { loadClass(StandardClassIds.KMutableProperty2) }

    override val functionClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Function) }
    override val kFunctionClass: IrClassSymbol by lazy { loadClass(StandardClassIds.KFunction) }

    override val primitiveTypeToIrType by lazy {
        mapOf(
            PrimitiveType.BOOLEAN to booleanType,
            PrimitiveType.CHAR to charType,
            PrimitiveType.BYTE to byteType,
            PrimitiveType.SHORT to shortType,
            PrimitiveType.INT to intType,
            PrimitiveType.LONG to longType,
            PrimitiveType.FLOAT to floatType,
            PrimitiveType.DOUBLE to doubleType
        )
    }

    private val primitiveIntegralIrTypes by lazy { listOf(byteType, shortType, intType, longType) }
    override val primitiveFloatingPointIrTypes by lazy { listOf(floatType, doubleType) }
    private val primitiveNumericIrTypes by lazy { primitiveIntegralIrTypes + primitiveFloatingPointIrTypes }
    override val primitiveIrTypesWithComparisons by lazy { listOf(charType) + primitiveNumericIrTypes }
    override val primitiveIrTypes by lazy { listOf(booleanType) + primitiveIrTypesWithComparisons }
    private val baseIrTypes by lazy { primitiveIrTypes + stringType }

    private fun primitiveIterator(primitiveType: PrimitiveType): IrClassSymbol {
        return loadClass(ClassId(StandardClassIds.BASE_COLLECTIONS_PACKAGE, Name.identifier("${primitiveType.typeName}Iterator")))
    }

    override val booleanIterator by lazy { primitiveIterator(PrimitiveType.BOOLEAN) }
    override val charIterator by lazy { primitiveIterator(PrimitiveType.CHAR) }
    override val byteIterator by lazy { primitiveIterator(PrimitiveType.BYTE) }
    override val shortIterator by lazy { primitiveIterator(PrimitiveType.SHORT) }
    override val intIterator by lazy { primitiveIterator(PrimitiveType.INT) }
    override val longIterator by lazy { primitiveIterator(PrimitiveType.LONG) }
    override val floatIterator by lazy { primitiveIterator(PrimitiveType.FLOAT) }
    override val doubleIterator by lazy { primitiveIterator(PrimitiveType.DOUBLE) }

    private fun loadPrimitiveArray(primitiveType: PrimitiveType): IrClassSymbol {
        return loadClass(ClassId(StandardClassIds.BASE_KOTLIN_PACKAGE, Name.identifier("${primitiveType.typeName}Array")))
    }

    override val booleanArray: IrClassSymbol by lazy { loadPrimitiveArray(PrimitiveType.BOOLEAN) }
    override val charArray: IrClassSymbol by lazy { loadPrimitiveArray(PrimitiveType.CHAR) }
    override val byteArray: IrClassSymbol by lazy { loadPrimitiveArray(PrimitiveType.BYTE) }
    override val shortArray: IrClassSymbol by lazy { loadPrimitiveArray(PrimitiveType.SHORT) }
    override val intArray: IrClassSymbol by lazy { loadPrimitiveArray(PrimitiveType.INT) }
    override val longArray: IrClassSymbol by lazy { loadPrimitiveArray(PrimitiveType.LONG) }
    override val floatArray: IrClassSymbol by lazy { loadPrimitiveArray(PrimitiveType.FLOAT) }
    override val doubleArray: IrClassSymbol by lazy { loadPrimitiveArray(PrimitiveType.DOUBLE) }

    override val primitiveArraysToPrimitiveTypes: Map<IrClassSymbol, PrimitiveType> by lazy {
        mapOf(
            booleanArray to PrimitiveType.BOOLEAN,
            charArray to PrimitiveType.CHAR,
            byteArray to PrimitiveType.BYTE,
            shortArray to PrimitiveType.SHORT,
            intArray to PrimitiveType.INT,
            longArray to PrimitiveType.LONG,
            floatArray to PrimitiveType.FLOAT,
            doubleArray to PrimitiveType.DOUBLE
        )
    }

    override val primitiveTypesToPrimitiveArrays get() = primitiveArraysToPrimitiveTypes.map { (k, v) -> v to k }.toMap()
    override val primitiveArrayElementTypes get() = primitiveArraysToPrimitiveTypes.mapValues { primitiveTypeToIrType[it.value] }
    override val primitiveArrayForType get() = primitiveArrayElementTypes.asSequence().associate { it.value to it.key }

    private val _ieee754equalsFunByOperandType = mutableMapOf<IrClassifierSymbol, IrSimpleFunctionSymbol>()
    override val ieee754equalsFunByOperandType: MutableMap<IrClassifierSymbol, IrSimpleFunctionSymbol>
        get() = _ieee754equalsFunByOperandType

    override val eqeqeqSymbol: IrSimpleFunctionSymbol
    override val eqeqSymbol: IrSimpleFunctionSymbol
    override val throwCceSymbol: IrSimpleFunctionSymbol
    override val throwIseSymbol: IrSimpleFunctionSymbol
    override val andandSymbol: IrSimpleFunctionSymbol
    override val ororSymbol: IrSimpleFunctionSymbol
    override val noWhenBranchMatchedExceptionSymbol: IrSimpleFunctionSymbol
    override val illegalArgumentExceptionSymbol: IrSimpleFunctionSymbol
    override val dataClassArrayMemberHashCodeSymbol: IrSimpleFunctionSymbol
    override val dataClassArrayMemberToStringSymbol: IrSimpleFunctionSymbol

    override val checkNotNullSymbol: IrSimpleFunctionSymbol
    override val arrayOfNulls: IrSimpleFunctionSymbol by lazy {
        val firSymbol = symbolProvider
            .getTopLevelFunctionSymbols(kotlinPackage, Name.identifier("arrayOfNulls")).first {
                it.fir.valueParameters.singleOrNull()?.returnTypeRef?.coneType?.isInt == true
            }
        findFunction(firSymbol)
    }

    override val linkageErrorSymbol: IrSimpleFunctionSymbol
        get() = TODO("Not yet implemented")

    override val lessFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
    override val lessOrEqualFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
    override val greaterOrEqualFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
    override val greaterFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>

    init {
        with(this.kotlinInternalIrPackageFragment) {

            fun addBuiltinOperatorSymbol(
                name: String,
                returnType: IrType,
                vararg valueParameterTypes: Pair<String, IrType>,
                isIntrinsicConst: Boolean = false,
            ): IrSimpleFunctionSymbol {
                return createFunction(
                    name, returnType, valueParameterTypes,
                    origin = BUILTIN_OPERATOR,
                    isIntrinsicConst = isIntrinsicConst
                ).also {
                    // `kotlinInternalIrPackageFragment` definitely is not a lazy class
                    @OptIn(UnsafeDuringIrConstructionAPI::class)
                    declarations.add(it)
                }.symbol
            }

            primitiveFloatingPointIrTypes.forEach { fpType ->
                _ieee754equalsFunByOperandType[fpType.classifierOrFail] = addBuiltinOperatorSymbol(
                    BuiltInOperatorNames.IEEE754_EQUALS,
                    booleanType,
                    "arg0" to fpType.makeNullable(),
                    "arg1" to fpType.makeNullable(),
                    isIntrinsicConst = true
                )
            }
            eqeqeqSymbol =
                addBuiltinOperatorSymbol(BuiltInOperatorNames.EQEQEQ, booleanType, "" to anyNType, "" to anyNType)
            eqeqSymbol =
                addBuiltinOperatorSymbol(BuiltInOperatorNames.EQEQ, booleanType, "" to anyNType, "" to anyNType, isIntrinsicConst = true)
            throwCceSymbol = addBuiltinOperatorSymbol(BuiltInOperatorNames.THROW_CCE, nothingType)
            throwIseSymbol = addBuiltinOperatorSymbol(BuiltInOperatorNames.THROW_ISE, nothingType)
            andandSymbol =
                addBuiltinOperatorSymbol(
                    BuiltInOperatorNames.ANDAND,
                    booleanType,
                    "" to booleanType,
                    "" to booleanType,
                    isIntrinsicConst = true
                )
            ororSymbol =
                addBuiltinOperatorSymbol(
                    BuiltInOperatorNames.OROR,
                    booleanType,
                    "" to booleanType,
                    "" to booleanType,
                    isIntrinsicConst = true
                )
            noWhenBranchMatchedExceptionSymbol =
                addBuiltinOperatorSymbol(BuiltInOperatorNames.NO_WHEN_BRANCH_MATCHED_EXCEPTION, nothingType)
            illegalArgumentExceptionSymbol =
                addBuiltinOperatorSymbol(BuiltInOperatorNames.ILLEGAL_ARGUMENT_EXCEPTION, nothingType, "" to stringType)
            dataClassArrayMemberHashCodeSymbol = addBuiltinOperatorSymbol("dataClassArrayMemberHashCode", intType, "" to anyType)
            dataClassArrayMemberToStringSymbol = addBuiltinOperatorSymbol("dataClassArrayMemberToString", stringType, "" to anyNType)

            checkNotNullSymbol = run {
                val typeParameter: IrTypeParameter = irFactory.createTypeParameter(
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
                    BuiltInOperatorNames.CHECK_NOT_NULL,
                    IrSimpleTypeImpl(typeParameter.symbol, SimpleTypeNullability.DEFINITELY_NOT_NULL, emptyList(), emptyList()),
                    arrayOf("" to IrSimpleTypeImpl(typeParameter.symbol, hasQuestionMark = true, emptyList(), emptyList())),
                    typeParameters = listOf(typeParameter),
                    origin = BUILTIN_OPERATOR
                ).also {
                    // `kotlinInternalIrPackageFragment` definitely is not a lazy class
                    @OptIn(UnsafeDuringIrConstructionAPI::class)
                    declarations.add(it)
                }.symbol
            }

            fun List<IrType>.defineComparisonOperatorForEachIrType(name: String) =
                associate {
                    it.classifierOrFail to addBuiltinOperatorSymbol(
                        name,
                        booleanType,
                        "" to it,
                        "" to it,
                        isIntrinsicConst = true
                    )
                }

            lessFunByOperandType = primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(BuiltInOperatorNames.LESS)
            lessOrEqualFunByOperandType =
                primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(BuiltInOperatorNames.LESS_OR_EQUAL)
            greaterOrEqualFunByOperandType =
                primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(BuiltInOperatorNames.GREATER_OR_EQUAL)
            greaterFunByOperandType = primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(BuiltInOperatorNames.GREATER)

        }
    }

    override val unsignedTypesToUnsignedArrays: Map<UnsignedType, IrClassSymbol> by lazy {
        UnsignedType.entries.mapNotNull { unsignedType ->
            val array = loadClassSafe(unsignedType.arrayClassId)
            if (array == null) null else unsignedType to array
        }.toMap()
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override val unsignedArraysElementTypes: Map<IrClassSymbol, IrType?> by lazy {
        unsignedTypesToUnsignedArrays.map { (k, v) -> v to loadClass(k.classId).owner.defaultType }.toMap()
    }

    override fun getKPropertyClass(mutable: Boolean, n: Int): IrClassSymbol = when (n) {
        0 -> if (mutable) kMutableProperty0Class else kProperty0Class
        1 -> if (mutable) kMutableProperty1Class else kProperty1Class
        2 -> if (mutable) kMutableProperty2Class else kProperty2Class
        else -> error("No KProperty for n=$n mutable=$mutable")
    }

    override val enumClass: IrClassSymbol by lazy { loadClass(StandardClassIds.Enum) }

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
        findFunction(firFunctionSymbol)
    }

    override val memberToString: IrSimpleFunctionSymbol by lazy {
        val firFunction = findFirMemberFunctions(StandardClassIds.Any, OperatorNameConventions.TO_STRING).single {
            it.fir.valueParameters.isEmpty()
        }
        findFunction(firFunction)
    }

    override val extensionStringPlus: IrSimpleFunctionSymbol by lazy {
        val firFunction = symbolProvider.getTopLevelFunctionSymbols(kotlinPackage, OperatorNameConventions.PLUS).single { symbol ->
            val isStringExtension = symbol.fir.receiverParameter?.typeRef?.coneType?.isNullableString == true
            isStringExtension && symbol.fir.valueParameters.singleOrNull { it.returnTypeRef.coneType.isNullableAny } != null
        }
        findFunction(firFunction)
    }

    override val memberStringPlus: IrSimpleFunctionSymbol by lazy {
        val firFunction = findFirMemberFunctions(StandardClassIds.String, OperatorNameConventions.PLUS).single {
            it.fir.valueParameters.singleOrNull()?.returnTypeRef?.coneType?.isNullableAny == true
        }
        findFunction(firFunction)
    }

    override val arrayOf: IrSimpleFunctionSymbol by lazy {
        // distinct() is needed because we can get two Fir symbols for arrayOf function (from builtins and from stdlib)
        //   with the same IR symbol for them
        findFunctions(kotlinPackage, Name.identifier("arrayOf")).distinct().single()
    }

    override fun getNonBuiltInFunctionsByExtensionReceiver(
        name: Name,
        vararg packageNameSegments: String,
    ): Map<IrClassifierSymbol, IrSimpleFunctionSymbol> {
        return getFunctionsByKey(
            name,
            *packageNameSegments,
            mapKey = { symbol ->
                with(components) { symbol.fir.receiverParameter?.typeRef?.toIrType(typeConverter)?.classifierOrNull }
            },
            mapValue = { _, irSymbol -> irSymbol }
        )
    }

    override fun getNonBuiltinFunctionsByReturnType(
        name: Name,
        vararg packageNameSegments: String,
    ): Map<IrClassifierSymbol, IrSimpleFunctionSymbol> {
        return getFunctionsByKey(
            name,
            *packageNameSegments,
            mapKey = { with(components) { it.fir.returnTypeRef.toIrType(typeConverter).classifierOrNull } },
            mapValue = { _, irSymbol -> irSymbol }
        )
    }

    fun getNonBuiltInFunctionsWithFirCounterpartByExtensionReceiver(
        name: Name,
        vararg packageNameSegments: String,
    ): Map<IrClassifierSymbol, Pair<FirNamedFunctionSymbol, IrSimpleFunctionSymbol>> {
        return getFunctionsByKey(
            name,
            *packageNameSegments,
            mapKey = { symbol ->
                with(components) { symbol.fir.receiverParameter?.typeRef?.toIrType(typeConverter)?.classifierOrNull }
            },
            mapValue = { firSymbol, irSymbol -> firSymbol to irSymbol }
        )
    }

    private val functionNMap = mutableMapOf<Int, IrClass>()
    private val kFunctionNMap = mutableMapOf<Int, IrClass>()
    private val suspendFunctionNMap = mutableMapOf<Int, IrClass>()
    private val kSuspendFunctionNMap = mutableMapOf<Int, IrClass>()

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun functionN(arity: Int): IrClass = functionNMap.getOrPut(arity) {
        loadClass(StandardClassIds.FunctionN(arity)).owner
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun kFunctionN(arity: Int): IrClass = kFunctionNMap.getOrPut(arity) {
        loadClass(StandardClassIds.KFunctionN(arity)).owner
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun suspendFunctionN(arity: Int): IrClass = suspendFunctionNMap.getOrPut(arity) {
        loadClass(StandardClassIds.SuspendFunctionN(arity)).owner
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun kSuspendFunctionN(arity: Int): IrClass = kSuspendFunctionNMap.getOrPut(arity) {
        loadClass(StandardClassIds.KSuspendFunctionN(arity)).owner
    }

    override fun findFunctions(name: Name, vararg packageNameSegments: String): Iterable<IrSimpleFunctionSymbol> =
        findFunctions(FqName.fromSegments(packageNameSegments.asList()), name)

    override fun findFunctions(name: Name, packageFqName: FqName): Iterable<IrSimpleFunctionSymbol> =
        findFunctions(packageFqName, name)

    override fun findProperties(name: Name, packageFqName: FqName): Iterable<IrPropertySymbol> =
        findProperties(packageFqName, name)

    override fun findClass(name: Name, vararg packageNameSegments: String): IrClassSymbol? =
        loadClassSafe(FqName.fromSegments(packageNameSegments.asList()), name)

    override fun findClass(name: Name, packageFqName: FqName): IrClassSymbol? =
        loadClassSafe(packageFqName, name)

    private fun loadClassSafe(packageName: FqName, identifier: Name): IrClassSymbol? {
        return loadClassSafe(ClassId(packageName, identifier))
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

// ---------------

    private fun loadClassSafe(topLevelFqName: FqName): IrClassSymbol? {
        return loadClassSafe(ClassId.topLevel(topLevelFqName))
    }

    private fun loadClass(classId: ClassId): IrClassSymbol {
        return loadClassSafe(classId) ?: error("Class not found: $classId")
    }

    private fun loadClassSafe(classId: ClassId): IrClassSymbol? {
        val firClassSymbol = symbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol ?: return null
        return components.classifierStorage.getOrCreateIrClass(firClassSymbol).symbol
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrType.getMaybeBuiltinClass(): IrClass? {
        val lhsClassFqName = classFqName!!
        return baseIrTypes.find { it.classFqName == lhsClassFqName }?.getClass()
            ?: loadClassSafe(lhsClassFqName)?.owner
    }

    private fun createPackage(fqName: FqName): IrExternalPackageFragment =
        IrExternalPackageFragmentImpl.createEmptyExternalPackageFragment(moduleDescriptor, fqName)

    private fun IrDeclarationParent.createFunction(
        name: String,
        returnType: IrType,
        valueParameterTypes: Array<out Pair<String, IrType>>,
        typeParameters: List<IrTypeParameter> = emptyList(),
        origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
        modality: Modality = Modality.FINAL,
        isOperator: Boolean = false,
        isInfix: Boolean = false,
        isIntrinsicConst: Boolean = false,
        postBuild: IrSimpleFunction.() -> Unit = {},
        build: IrFunctionBuilder.() -> Unit = {},
    ): IrSimpleFunction {

        fun makeWithSymbol(symbol: IrSimpleFunctionSymbol) = IrFunctionBuilder().run {
            this.name = Name.identifier(name)
            this.returnType = returnType
            this.origin = origin
            this.modality = modality
            this.isOperator = isOperator
            this.isInfix = isInfix
            build()
            irFactory.createSimpleFunction(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = this.origin,
                name = this.name,
                visibility = visibility,
                isInline = isInline,
                isExpect = isExpect,
                returnType = this.returnType,
                modality = this.modality,
                symbol = symbol,
                isTailrec = isTailrec,
                isSuspend = isSuspend,
                isOperator = this.isOperator,
                isInfix = this.isInfix,
                isExternal = isExternal,
                containerSource = containerSource,
                isFakeOverride = isFakeOverride,
            )
        }.also { fn ->
            valueParameterTypes.forEachIndexed { index, (pName, irType) ->
                fn.addValueParameter(Name.identifier(pName.ifBlank { "arg$index" }), irType, origin)
            }
            fn.typeParameters = typeParameters
            typeParameters.forEach { it.parent = fn }
            if (isIntrinsicConst) {
                fn.annotations += intrinsicConstAnnotation
            }
            fn.parent = this@createFunction
            fn.postBuild()
        }

        val irFun4SignatureCalculation = makeWithSymbol(IrSimpleFunctionSymbolImpl())
        val signature = irSignatureBuilder.computeSignature(irFun4SignatureCalculation)
        return components.symbolTable.declareSimpleFunction(
            signature,
            { IrSimpleFunctionPublicSymbolImpl(signature, null) },
            ::makeWithSymbol
        )
    }

    private fun findFunctions(packageName: FqName, name: Name): List<IrSimpleFunctionSymbol> {
        return symbolProvider.getTopLevelFunctionSymbols(packageName, name).map { findFunction(it) }
    }

    private inline fun <K : Any, T> getFunctionsByKey(
        name: Name,
        vararg packageNameSegments: String,
        mapKey: (FirNamedFunctionSymbol) -> K?,
        mapValue: (FirNamedFunctionSymbol, IrSimpleFunctionSymbol) -> T
    ): Map<K, T> {
        val packageName = FqName.fromSegments(packageNameSegments.asList())
        val result = mutableMapOf<K, T>()
        for (functionSymbol in symbolProvider.getTopLevelFunctionSymbols(packageName, name)) {
            val key = mapKey(functionSymbol) ?: continue
            val irFunctionSymbol = findFunction(functionSymbol)
            result[key] = mapValue(functionSymbol, irFunctionSymbol)
        }
        return result
    }

    private fun findFunction(functionSymbol: FirNamedFunctionSymbol): IrSimpleFunctionSymbol {
        functionSymbol.lazyResolveToPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
        return components.declarationStorage.getIrFunctionSymbol(functionSymbol) as IrSimpleFunctionSymbol
    }

    private fun findProperties(packageName: FqName, name: Name): List<IrPropertySymbol> {
        return symbolProvider.getTopLevelPropertySymbols(packageName, name).map { findProperty(it) }
    }

    private fun findProperty(propertySymbol: FirPropertySymbol): IrPropertySymbol {
        propertySymbol.lazyResolveToPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
        return components.declarationStorage.getIrPropertySymbol(propertySymbol) as IrPropertySymbol
    }

    private val IrClassSymbol.defaultTypeWithoutArguments: IrSimpleType
        get() = IrSimpleTypeImpl(
            kotlinType = null,
            classifier = this,
            nullability = SimpleTypeNullability.DEFINITELY_NOT_NULL,
            arguments = emptyList(),
            annotations = emptyList()
        )
}
