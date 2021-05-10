/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.backend.common.ir.addFakeOverrides
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.ir.BuiltInOperatorNames
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassPublicSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorPublicSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionPublicSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions

class IrBuiltInsOverFir(
    private val components: Fir2IrComponents,
    override val languageVersionSettings: LanguageVersionSettings,
    private val moduleDescriptor: FirModuleDescriptor
) : IrBuiltIns() {

    override val irFactory: IrFactory = components.symbolTable.irFactory

    private val kotlinPackage = StandardNames.BUILT_INS_PACKAGE_FQ_NAME
    private val kotlinReflectPackage = StandardNames.KOTLIN_REFLECT_FQ_NAME

    private val kotlinCollectionsPackage = StandardNames.COLLECTIONS_PACKAGE_FQ_NAME

    private val internalIrPackage = createPackage(KOTLIN_INTERNAL_IR_FQN)
    private val kotlinIrPackage = createPackage(kotlinPackage)
    private val kotlinCollectionsIrPackage = createPackage(kotlinCollectionsPackage)

    override lateinit var booleanNotSymbol: IrSimpleFunctionSymbol private set

    override val anyClass: IrClassSymbol = kotlinIrPackage.createClass(IdSignatureValues.any) {
        createConstructor()
        createMemberFunction(OperatorNameConventions.EQUALS, booleanType, "other" to defaultType.withHasQuestionMark(true), modality = Modality.OPEN, isOperator = true)
        createMemberFunction("hashCode", intType, modality = Modality.OPEN)
        createMemberFunction("toString", stringType, modality = Modality.OPEN)
    }
    override val anyType: IrType = anyClass.defaultType
    override val anyNType = anyType.withHasQuestionMark(true)

    override val numberClass: IrClassSymbol = kotlinIrPackage.createClass(IdSignatureValues.number, classModality = Modality.ABSTRACT)
    override val numberType: IrType get() = numberClass.defaultType

    override val nothingClass: IrClassSymbol = kotlinIrPackage.createClass(IdSignatureValues.nothing)
    override val nothingType: IrType get() = nothingClass.defaultType
    override val nothingNType: IrType = nothingType.withHasQuestionMark(true)

    override val unitClass: IrClassSymbol = kotlinIrPackage.createClass(IdSignatureValues.unit, classKind = ClassKind.OBJECT, classModality = Modality.FINAL)
    override val unitType: IrType get() = unitClass.defaultType

    override val booleanType: IrType get() = booleanClass.defaultType
    override val booleanClass: IrClassSymbol = kotlinIrPackage.createClass(IdSignatureValues._boolean)

    override val charType: IrType get() = charClass.defaultType
    override val charClass: IrClassSymbol = kotlinIrPackage.createClass(IdSignatureValues._char)

    override val byteType: IrType get() = byteClass.defaultType
    override val byteClass: IrClassSymbol = kotlinIrPackage.createNumberClass(IdSignatureValues._byte)
    override val shortType: IrType get() = shortClass.defaultType
    override val shortClass: IrClassSymbol = kotlinIrPackage.createNumberClass(IdSignatureValues._short)
    override val intType: IrType get() = intClass.defaultType
    override val intClass: IrClassSymbol = kotlinIrPackage.createNumberClass(IdSignatureValues._int)
    override val longType: IrType get() = longClass.defaultType
    override val longClass: IrClassSymbol = kotlinIrPackage.createNumberClass(IdSignatureValues._long)
    override val floatType: IrType get() = floatClass.defaultType
    override val floatClass: IrClassSymbol = kotlinIrPackage.createNumberClass(IdSignatureValues._float)
    override val doubleType: IrType get() = doubleClass.defaultType
    override val doubleClass: IrClassSymbol = kotlinIrPackage.createNumberClass(IdSignatureValues._double)

    override val charSequenceClass: IrClassSymbol = kotlinIrPackage.createClass(IdSignatureValues.charSequence, classKind = ClassKind.INTERFACE)

    override val stringClass: IrClassSymbol = kotlinIrPackage.createClass(IdSignatureValues.string, charSequenceClass.defaultType)
    override val stringType: IrType get() = stringClass.defaultType

    override val arrayClass: IrClassSymbol = kotlinIrPackage.createClass(IdSignatureValues.array) klass@{
        val typeParameter = addTypeParameter("T", anyNType)
        addArrayMembers(typeParameter.defaultType)
    }

    override val annotationClass: IrClassSymbol by lazy { referenceClassByFqname(kotlinPackage, "Annotation")!! }
    override val annotationType: IrType get() = annotationClass.defaultType

    override val collectionClass: IrClassSymbol by lazy { referenceClassByFqname(kotlinCollectionsPackage, "Collection")!! }
    override val setClass: IrClassSymbol by lazy { referenceClassByFqname(kotlinCollectionsPackage, "Set")!! }
    override val listClass: IrClassSymbol by lazy { referenceClassByFqname(kotlinCollectionsPackage, "List")!! }
    override val mapClass: IrClassSymbol by lazy { referenceClassByFqname(kotlinCollectionsPackage, "Map")!! }
    override val mapEntryClass: IrClassSymbol by lazy { referenceNestedClass(mapClass, "Entry")!! }

    override val iterableClass: IrClassSymbol by lazy { referenceClassByFqname(StandardNames.FqNames.iterable)!! }
    override val listIteratorClass: IrClassSymbol by lazy { referenceClassByFqname(StandardNames.FqNames.listIterator)!! }
    override val mutableCollectionClass: IrClassSymbol by lazy { referenceClassByFqname(StandardNames.FqNames.mutableCollection)!! }
    override val mutableSetClass: IrClassSymbol by lazy { referenceClassByFqname(StandardNames.FqNames.mutableSet)!! }
    override val mutableListClass: IrClassSymbol by lazy { referenceClassByFqname(StandardNames.FqNames.mutableList)!! }
    override val mutableMapClass: IrClassSymbol by lazy { referenceClassByFqname(StandardNames.FqNames.mutableMap)!! }
    override val mutableMapEntryClass: IrClassSymbol by lazy { referenceNestedClass(StandardNames.FqNames.mutableMapEntry)!! }
    override val mutableIterableClass: IrClassSymbol by lazy { referenceClassByFqname(StandardNames.FqNames.mutableIterable)!! }
    override val mutableIteratorClass: IrClassSymbol by lazy { referenceClassByFqname(StandardNames.FqNames.mutableIterator)!! }
    override val mutableListIteratorClass: IrClassSymbol by lazy { referenceClassByFqname(StandardNames.FqNames.mutableListIterator)!! }
    override val comparableClass: IrClassSymbol by lazy { referenceClassByFqname(StandardNames.FqNames.comparable)!! }
    override val throwableType: IrType by lazy { throwableClass.defaultType }
    override val throwableClass: IrClassSymbol by lazy { referenceClassByFqname(StandardNames.FqNames.throwable)!! }

    override val kCallableClass: IrClassSymbol by lazy { referenceClassByFqname(StandardNames.FqNames.kCallable.toSafe())!! }
    override val kPropertyClass: IrClassSymbol by lazy { referenceClassByFqname(StandardNames.FqNames.kPropertyFqName.toSafe())!! }
    override val kClassClass: IrClassSymbol by lazy { referenceClassByFqname(StandardNames.FqNames.kClass.toSafe())!! }
    override val kProperty0Class: IrClassSymbol by lazy { referenceClassByFqname(StandardNames.FqNames.kProperty0.toSafe())!! }
    override val kProperty1Class: IrClassSymbol by lazy { referenceClassByFqname(StandardNames.FqNames.kProperty1.toSafe())!! }
    override val kProperty2Class: IrClassSymbol by lazy { referenceClassByFqname(StandardNames.FqNames.kProperty2.toSafe())!! }
    override val kMutableProperty0Class: IrClassSymbol by lazy { referenceClassByFqname(StandardNames.FqNames.kMutableProperty0.toSafe())!! }
    override val kMutableProperty1Class: IrClassSymbol by lazy { referenceClassByFqname(StandardNames.FqNames.kMutableProperty1.toSafe())!! }
    override val kMutableProperty2Class: IrClassSymbol by lazy { referenceClassByFqname(StandardNames.FqNames.kMutableProperty2.toSafe())!! }

    override val functionClass: IrClassSymbol by lazy { referenceClassByFqname(kotlinPackage, "Function")!! }
    override val kFunctionClass: IrClassSymbol by lazy { referenceClassByFqname(kotlinReflectPackage, "KFunction")!! }

    override val primitiveTypeToIrType = mapOf(
        PrimitiveType.BOOLEAN to booleanType,
        PrimitiveType.CHAR to charType,
        PrimitiveType.BYTE to byteType,
        PrimitiveType.SHORT to shortType,
        PrimitiveType.INT to intType,
        PrimitiveType.LONG to longType,
        PrimitiveType.FLOAT to floatType,
        PrimitiveType.DOUBLE to doubleType
    )

    private val primitiveIntegralIrTypes = listOf(byteType, shortType, intType, longType)
    override val primitiveFloatingPointIrTypes = listOf(floatType, doubleType)
    private val primitiveNumericIrTypes = primitiveIntegralIrTypes + primitiveFloatingPointIrTypes
    override val primitiveIrTypesWithComparisons = listOf(charType) + primitiveNumericIrTypes
    override val primitiveIrTypes = listOf(booleanType) + primitiveIrTypesWithComparisons
    private val baseIrTypes = primitiveIrTypes + stringType

    private fun getPrimitiveArithmeticOperatorResultType(target: IrType, arg: IrType) =
        when {
            arg == doubleType -> arg
            target in primitiveFloatingPointIrTypes -> target
            arg in primitiveFloatingPointIrTypes -> arg
            target == longType -> target
            arg == longType -> arg
            else -> intType
        }

    init {
        val intRange = referenceClassByFqname(StandardNames.RANGES_PACKAGE_FQ_NAME, "IntRange")!!.owner.defaultType
        val longRange = referenceClassByFqname(StandardNames.RANGES_PACKAGE_FQ_NAME, "LongRange")!!.owner.defaultType
        val charRange = referenceClassByFqname(StandardNames.RANGES_PACKAGE_FQ_NAME, "CharRange")!!.owner.defaultType

        with (anyClass.owner) {
            createConstructor()
            createMemberFunction(OperatorNameConventions.EQUALS, booleanType, "other" to defaultType.withHasQuestionMark(true), modality = Modality.OPEN, isOperator = true)
            createMemberFunction("hashCode", intType, modality = Modality.OPEN)
            createMemberFunction("toString", stringType, modality = Modality.OPEN)
        }

        for (klass in arrayOf(booleanClass, charClass, numberClass, charSequenceClass, stringClass)) {
            klass.owner.superTypes += anyType
        }

        with (booleanClass.owner) {
            booleanNotSymbol = createMemberFunction(OperatorNameConventions.NOT, defaultType, isOperator = true).symbol
            createMemberFunction(OperatorNameConventions.AND, defaultType, "other" to defaultType) { isInfix = true }
            createMemberFunction(OperatorNameConventions.OR, defaultType, "other" to defaultType) { isInfix = true }
            createMemberFunction(OperatorNameConventions.XOR, defaultType, "other" to defaultType) { isInfix = true }
            createMemberFunction(OperatorNameConventions.COMPARE_TO, intType, "other" to booleanType, modality = Modality.OPEN, isOperator = true)
            addFakeOverrides(this@IrBuiltInsOverFir)
        }

        with (arrayClass.owner) {
            val typeParameter = addTypeParameter("T", anyNType)
            addArrayMembers(typeParameter.defaultType)
            addFakeOverrides(this@IrBuiltInsOverFir)
        }

        with (numberClass.owner) {
            for (targetPrimitive in primitiveIrTypesWithComparisons) {
                createMemberFunction("to${targetPrimitive.classFqName!!.shortName().asString()}", targetPrimitive, modality = Modality.ABSTRACT)
            }
            addFakeOverrides(this@IrBuiltInsOverFir)
        }

        for (numericOrChar in primitiveNumericIrTypes + charType) {
            with(numericOrChar.getClass()!!) {
                createCompanionObject() {
                    val constExprs = getNumericConstantsExpressions(numericOrChar)
                    createProperty("MIN_VALUE", numericOrChar, isConst = true, withGetter = false, fieldInit = constExprs.min)
                    createProperty("MAX_VALUE", numericOrChar, isConst = true, withGetter = false, fieldInit = constExprs.max)
                    createProperty("SIZE_BYTES", intType, isConst = true, withGetter = false, fieldInit = constExprs.sizeBytes)
                    createProperty("SIZE_BITS", intType, isConst = true, withGetter = false, fieldInit = constExprs.sizeBits)
                }
                for (targetPrimitive in primitiveIrTypesWithComparisons) {
                    createMemberFunction("to${targetPrimitive.classFqName!!.shortName().asString()}", targetPrimitive, modality = Modality.OPEN)
                }
                createMemberFunction(OperatorNameConventions.INC, numericOrChar, isOperator = true)
                createMemberFunction(OperatorNameConventions.DEC, numericOrChar, isOperator = true)
                addFakeOverrides(this@IrBuiltInsOverFir)
            }
        }
        for (numeric in primitiveNumericIrTypes) {
            with(numeric.getClass()!!) {
                for (argument in primitiveNumericIrTypes) {
                    createMemberFunction(
                        OperatorNameConventions.COMPARE_TO, intType, "other" to argument,
                        modality = if (argument == numeric) Modality.OPEN else Modality.FINAL,
                        isOperator = true
                    )
                    val targetArithmeticReturnType = getPrimitiveArithmeticOperatorResultType(numeric, argument)
                    for (op in arrayOf(
                        OperatorNameConventions.PLUS,
                        OperatorNameConventions.MINUS,
                        OperatorNameConventions.TIMES,
                        OperatorNameConventions.DIV,
                        OperatorNameConventions.REM
                    )) {
                        createMemberFunction(op, targetArithmeticReturnType, "other" to argument, isOperator = true)
                    }
                }
                val arithmeticReturnType = getPrimitiveArithmeticOperatorResultType(numeric, numeric)
                createMemberFunction(OperatorNameConventions.UNARY_PLUS, arithmeticReturnType, isOperator = true)
                createMemberFunction(OperatorNameConventions.UNARY_MINUS, arithmeticReturnType, isOperator = true)
            }
        }
        for (integral in primitiveIntegralIrTypes) {
            with(integral.getClass()!!) {
                for (argType in primitiveIntegralIrTypes) {
                    createMemberFunction(
                        OperatorNameConventions.RANGE_TO,
                        if (integral == longType || argType == longType) longRange else intRange,
                        "other" to argType, isOperator = true
                    )
                }
            }
        }
        for (typeWithBitwiseOps in arrayOf(intType, longType)) {
            with(typeWithBitwiseOps.getClass()!!) {
                for (op in arrayOf(OperatorNameConventions.AND, OperatorNameConventions.OR, OperatorNameConventions.XOR)) {
                    createMemberFunction(op, typeWithBitwiseOps, "other" to typeWithBitwiseOps, isOperator = true)
                }
                for (op in arrayOf(OperatorNameConventions.SHL, OperatorNameConventions.SHR, OperatorNameConventions.USHR)) {
                    createMemberFunction(op, typeWithBitwiseOps, "bitCount" to intType, isOperator = true)
                }
                createMemberFunction(OperatorNameConventions.INV, typeWithBitwiseOps, isOperator = true)
            }
        }
        with(charClass.owner) {
            createMemberFunction(OperatorNameConventions.COMPARE_TO, intType, "other" to charType, modality = Modality.OPEN, isOperator = true)
            createMemberFunction(OperatorNameConventions.PLUS, charType, "other" to intType, isOperator = true)
            createMemberFunction(OperatorNameConventions.MINUS, charType, "other" to intType, isOperator = true)
            createMemberFunction(OperatorNameConventions.MINUS, intType, "other" to charType, isOperator = true)
            createMemberFunction(OperatorNameConventions.RANGE_TO, charRange, "other" to charType)
        }
        with(charSequenceClass.owner) {
            createProperty("length", intType, modality = Modality.ABSTRACT)
            createMemberFunction(OperatorNameConventions.GET, charType, "index" to intType, modality = Modality.ABSTRACT, isOperator = true)
            createMemberFunction("subSequence", defaultType, "startIndex" to intType, "endIndex" to intType, modality = Modality.ABSTRACT)
            addFakeOverrides(this@IrBuiltInsOverFir)
        }
        with(stringClass.owner) {
            createProperty("length", intType, modality = Modality.OPEN)
            createMemberFunction(OperatorNameConventions.GET, charType, "index" to intType, modality = Modality.OPEN, isOperator = true)
            createMemberFunction("subSequence", charSequenceClass.defaultType, "startIndex" to intType, "endIndex" to intType, modality = Modality.OPEN)
            createMemberFunction(OperatorNameConventions.COMPARE_TO, intType, "other" to defaultType, modality = Modality.OPEN, isOperator = true)
            createMemberFunction(OperatorNameConventions.PLUS, defaultType, "other" to anyNType, isOperator = true)
            addFakeOverrides(this@IrBuiltInsOverFir)
        }
    }

    override val booleanArray: IrClassSymbol =
        kotlinIrPackage.createClass(PrimitiveType.BOOLEAN.arrayTypeName) { addArrayMembers(booleanType) }
    override val charArray: IrClassSymbol = kotlinIrPackage.createClass(PrimitiveType.CHAR.arrayTypeName) { addArrayMembers(charType) }
    override val byteArray: IrClassSymbol = kotlinIrPackage.createClass(PrimitiveType.BYTE.arrayTypeName) { addArrayMembers(byteType) }
    override val shortArray: IrClassSymbol = kotlinIrPackage.createClass(PrimitiveType.SHORT.arrayTypeName) { addArrayMembers(shortType) }
    override val intArray: IrClassSymbol = kotlinIrPackage.createClass(PrimitiveType.INT.arrayTypeName) { addArrayMembers(intType) }
    override val longArray: IrClassSymbol = kotlinIrPackage.createClass(PrimitiveType.LONG.arrayTypeName) { addArrayMembers(longType) }
    override val floatArray: IrClassSymbol = kotlinIrPackage.createClass(PrimitiveType.FLOAT.arrayTypeName) { addArrayMembers(floatType) }
    override val doubleArray: IrClassSymbol =
        kotlinIrPackage.createClass(PrimitiveType.DOUBLE.arrayTypeName) { addArrayMembers(doubleType) }

    override val primitiveArraysToPrimitiveTypes: Map<IrClassSymbol, PrimitiveType> = mapOf(
        booleanArray to PrimitiveType.BOOLEAN,
        charArray to PrimitiveType.CHAR,
        byteArray to PrimitiveType.BYTE,
        shortArray to PrimitiveType.SHORT,
        intArray to PrimitiveType.INT,
        longArray to PrimitiveType.LONG,
        floatArray to PrimitiveType.FLOAT,
        doubleArray to PrimitiveType.DOUBLE
    )

    override val primitiveTypesToPrimitiveArrays get() = primitiveArraysToPrimitiveTypes.map { (k, v) -> v to k }.toMap()
    override val primitiveArrayElementTypes get() = primitiveArraysToPrimitiveTypes.mapValues { primitiveTypeToIrType[it.value] }
    override val primitiveArrayForType get() = primitiveArrayElementTypes.asSequence().associate { it.value to it.key }

    private val _ieee754equalsFunByOperandType = mutableMapOf<IrClassifierSymbol, IrSimpleFunctionSymbol>()
    override val ieee754equalsFunByOperandType: MutableMap<IrClassifierSymbol, IrSimpleFunctionSymbol>
        get() = _ieee754equalsFunByOperandType

    override lateinit var eqeqeqSymbol: IrSimpleFunctionSymbol private set
    override lateinit var eqeqSymbol: IrSimpleFunctionSymbol private set
    override lateinit var throwCceSymbol: IrSimpleFunctionSymbol private set
    override lateinit var throwIseSymbol: IrSimpleFunctionSymbol private set
    override lateinit var andandSymbol: IrSimpleFunctionSymbol private set
    override lateinit var ororSymbol: IrSimpleFunctionSymbol private set
    override lateinit var noWhenBranchMatchedExceptionSymbol: IrSimpleFunctionSymbol private set
    override lateinit var illegalArgumentExceptionSymbol: IrSimpleFunctionSymbol private set
    override lateinit var dataClassArrayMemberHashCodeSymbol: IrSimpleFunctionSymbol private set
    override lateinit var dataClassArrayMemberToStringSymbol: IrSimpleFunctionSymbol private set

    override lateinit var checkNotNullSymbol: IrSimpleFunctionSymbol private set
    override val arrayOfNulls: IrSimpleFunctionSymbol by lazy {
        findFunctions(kotlinPackage, Name.identifier("arrayOfNulls")).first {
            it.owner.dispatchReceiverParameter == null && it.owner.valueParameters.size == 1 &&
                    it.owner.valueParameters[0].type == intType
        }
    }

    override lateinit var lessFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> private set
    override lateinit var lessOrEqualFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> private set
    override lateinit var greaterOrEqualFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> private set
    override lateinit var greaterFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> private set

    init {
        with(internalIrPackage) {

            fun addBuiltinOperatorSymbol(
                name: String,
                returnType: IrType,
                vararg valueParameterTypes: Pair<String, IrType>,
                builder: IrSimpleFunction.() -> Unit = {}
            ) =
                createFunction(fqName, name, returnType, valueParameterTypes, origin = BUILTIN_OPERATOR).also {
                    declarations.add(it)
                    it.builder()
                }.symbol

            primitiveFloatingPointIrTypes.forEach { fpType ->
                _ieee754equalsFunByOperandType.put(
                    fpType.classifierOrFail,
                    addBuiltinOperatorSymbol(BuiltInOperatorNames.IEEE754_EQUALS, booleanType, "arg0" to fpType.makeNullable(), "arg1" to fpType.makeNullable())
                )
            }
            eqeqeqSymbol = addBuiltinOperatorSymbol(BuiltInOperatorNames.EQEQEQ, booleanType, "" to anyNType, "" to anyNType)
            eqeqSymbol = addBuiltinOperatorSymbol(BuiltInOperatorNames.EQEQ, booleanType, "" to anyNType, "" to anyNType)
            throwCceSymbol = addBuiltinOperatorSymbol(BuiltInOperatorNames.THROW_CCE, nothingType)
            throwIseSymbol = addBuiltinOperatorSymbol(BuiltInOperatorNames.THROW_ISE, nothingType)
            andandSymbol = addBuiltinOperatorSymbol(BuiltInOperatorNames.ANDAND, booleanType, "" to booleanType, "" to booleanType)
            ororSymbol = addBuiltinOperatorSymbol(BuiltInOperatorNames.OROR, booleanType, "" to booleanType, "" to booleanType)
            noWhenBranchMatchedExceptionSymbol =
                addBuiltinOperatorSymbol(BuiltInOperatorNames.NO_WHEN_BRANCH_MATCHED_EXCEPTION, nothingType)
            illegalArgumentExceptionSymbol =
                addBuiltinOperatorSymbol(BuiltInOperatorNames.ILLEGAL_ARGUMENT_EXCEPTION, nothingType, "" to stringType)
            dataClassArrayMemberHashCodeSymbol = addBuiltinOperatorSymbol("dataClassArrayMemberHashCode", intType, "" to anyType)
            dataClassArrayMemberToStringSymbol = addBuiltinOperatorSymbol("dataClassArrayMemberToString", stringType, "" to anyNType)

            checkNotNullSymbol = run {
                val typeParameter: IrTypeParameter = irFactory.createTypeParameter(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, BUILTIN_OPERATOR, IrTypeParameterSymbolImpl(), Name.identifier("T0"), 0, true,
                    Variance.INVARIANT
                ).apply {
                    superTypes += anyType
                }

                createFunction(
                    fqName, "CHECK_NOT_NULL",
                    IrSimpleTypeImpl(typeParameter.symbol, hasQuestionMark = false, emptyList(), emptyList()),
                    arrayOf("" to IrSimpleTypeImpl(typeParameter.symbol, hasQuestionMark = true, emptyList(), emptyList())),
                    origin = BUILTIN_OPERATOR
                ).also {
                    it.typeParameters = listOf(typeParameter)
                    typeParameter.parent = it
                    declarations.add(it)
                }.symbol
            }

            fun List<IrType>.defineComparisonOperatorForEachIrType(name: String) =
                associate { it.classifierOrFail to addBuiltinOperatorSymbol(name, booleanType, "" to it, "" to it) }

            lessFunByOperandType = primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(BuiltInOperatorNames.LESS)
            lessOrEqualFunByOperandType =
                primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(BuiltInOperatorNames.LESS_OR_EQUAL)
            greaterOrEqualFunByOperandType =
                primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(BuiltInOperatorNames.GREATER_OR_EQUAL)
            greaterFunByOperandType = primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(BuiltInOperatorNames.GREATER)

        }
    }

    override val unsignedTypesToUnsignedArrays: Map<UnsignedType, IrClassSymbol> by lazy {
        UnsignedType.values().mapNotNull { unsignedType ->
            val array = referenceClassByClassId(unsignedType.arrayClassId)
            if (array == null) null else unsignedType to array
        }.toMap()
    }

    override fun getKPropertyClass(mutable: Boolean, n: Int): IrClassSymbol = when (n) {
        0 -> if (mutable) kMutableProperty0Class else kProperty0Class
        1 -> if (mutable) kMutableProperty1Class else kProperty1Class
        2 -> if (mutable) kMutableProperty2Class else kProperty2Class
        else -> error("No KProperty for n=$n mutable=$mutable")
    }

    override val enumClass: IrClassSymbol by lazy { referenceClassByFqname(kotlinPackage, "Enum")!! }

    override val intPlusSymbol: IrSimpleFunctionSymbol
        get() = intClass.functions.single {
            it.owner.name == OperatorNameConventions.PLUS && it.owner.valueParameters[0].type == intType
        }

    override val intTimesSymbol: IrSimpleFunctionSymbol
        get() = intClass.functions.single {
            it.owner.name == OperatorNameConventions.TIMES && it.owner.valueParameters[0].type == intType
        }

    override val extensionToString: IrSimpleFunctionSymbol by lazy {
        findFunctions(kotlinPackage, Name.identifier("toString")).first { function ->
            function.owner.extensionReceiverParameter?.let { receiver -> receiver.type == anyNType } ?: false
        }
    }

    override val stringPlus: IrSimpleFunctionSymbol
        get() = intClass.functions.single {
            it.owner.name == OperatorNameConventions.PLUS && it.owner.valueParameters[0].type == stringType
        }

    private class KotlinPackageFuns(
        val arrayOf: IrSimpleFunctionSymbol,
    )

    private val kotlinBuiltinFunctions by lazy {
        fun IrClassSymbol.addPackageFun(
            name: String,
            returnType: IrType,
            vararg argumentTypes: Pair<String, IrType>,
            builder: IrSimpleFunction.() -> Unit
        ) =
            owner.createFunction(kotlinPackage, name, returnType, argumentTypes).also {
                it.builder()
                this.owner.declarations.add(it)
            }.symbol

        val kotlinKt = kotlinIrPackage.createClass(kotlinPackage.child(Name.identifier("KotlinKt")))
        KotlinPackageFuns(
            arrayOf = kotlinKt.addPackageFun("arrayOf", arrayClass.defaultType) arrayOf@ {
                addTypeParameter("T", anyNType)
                addValueParameter {
                    this.name = Name.identifier("elements")
                    this.type = arrayClass.defaultType
                    this.varargElementType = typeParameters[0].defaultType
                    this.origin = this@arrayOf.origin
                }
            }
        )
    }

    override val arrayOf: IrSimpleFunctionSymbol get() = kotlinBuiltinFunctions.arrayOf

    override val toUIntByExtensionReceiver: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> by lazy {
        findFunctions(kotlinPackage, Name.identifier("toUInt")).mapNotNull { fn ->
            fn.owner.extensionReceiverParameter?.type?.classifierOrNull?.let { klass ->
                klass to fn
            }
        }.toMap()
    }

    override val toULongByExtensionReceiver: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> by lazy {
        findFunctions(kotlinPackage, Name.identifier("toULong")).mapNotNull { fn ->
            fn.owner.extensionReceiverParameter?.type?.classifierOrNull?.let { klass ->
                klass to fn
            }
        }.toMap()
    }

    private val functionNMap = mutableMapOf<Int, IrClass>()
    private val kFunctionNMap = mutableMapOf<Int, IrClass>()
    private val suspendFunctionNMap = mutableMapOf<Int, IrClass>()
    private val kSuspendFunctionNMap = mutableMapOf<Int, IrClass>()

    override fun functionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass = functionN(arity)

    override fun kFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass = kFunctionN(arity)

    override fun suspendFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass =
        suspendFunctionN(arity)

    override fun kSuspendFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass =
        kSuspendFunctionN(arity)

    override fun functionN(arity: Int): IrClass = functionNMap.getOrPut(arity) {
        referenceClassByClassId(StandardNames.getFunctionClassId(arity))!!.owner
    }

    override fun kFunctionN(arity: Int): IrClass = kFunctionNMap.getOrPut(arity) {
        referenceClassByClassId(StandardNames.getKFunctionClassId(arity))!!.owner
    }

    override fun suspendFunctionN(arity: Int): IrClass = suspendFunctionNMap.getOrPut(arity) {
        referenceClassByClassId(StandardNames.getSuspendFunctionClassId(arity))!!.owner
    }

    override fun kSuspendFunctionN(arity: Int): IrClass = kSuspendFunctionNMap.getOrPut(arity) {
        referenceClassByClassId(StandardNames.getKSuspendFunctionClassId(arity))!!.owner
    }

    override fun findFunctions(name: Name, vararg packageNameSegments: String): Iterable<IrSimpleFunctionSymbol> =
        findFunctions(FqName.fromSegments(packageNameSegments.asList()), name)

    override fun findFunctions(name: Name, packageFqName: FqName): Iterable<IrSimpleFunctionSymbol> =
        findFunctions(packageFqName, name)

    override fun findClass(name: Name, vararg packageNameSegments: String): IrClassSymbol? =
        referenceClassByFqname(FqName.fromSegments(packageNameSegments.asList()), name)

    override fun findClass(name: Name, packageFqName: FqName): IrClassSymbol? =
        referenceClassByFqname(packageFqName, name)

    private val builtInClasses by lazy {
        setOf(anyClass)
    }

    override fun findBuiltInClassMemberFunctions(builtInClass: IrClassSymbol, name: Name): Iterable<IrSimpleFunctionSymbol> {
        require(builtInClass in builtInClasses)
        return builtInClass.functions.filter { it.owner.name == name }.asIterable()
    }

    override fun getBinaryOperator(name: Name, lhsType: IrType, rhsType: IrType): IrSimpleFunctionSymbol {
        val definingClass = lhsType.getMaybeBuiltinClass() ?: error("Defining class not found: $lhsType")
        return definingClass.functions.single { function ->
            function.name == name && function.valueParameters.size == 1 && function.valueParameters[0].type == rhsType
        }.symbol
    }

    override fun getUnaryOperator(name: Name, receiverType: IrType): IrSimpleFunctionSymbol {
        val definingClass = receiverType.getMaybeBuiltinClass() ?: error("Defining class not found: $receiverType")
        return definingClass.functions.single { function ->
            function.name == name && function.valueParameters.isEmpty()
        }.symbol
    }

    override val getProgressionLastElementByReturnType: Map<IrClassifierSymbol?, IrSimpleFunctionSymbol> by lazy {
        findFunctions(kotlinPackage.child(Name.identifier("internal")), Name.identifier("getProgressionLastElement")).mapNotNull { fn ->
            fn.owner.returnType.classOrNull?.let { it to fn }
        }.toMap()
    }

    private fun referenceClassByFqname(topLevelFqName: FqName) =
        referenceClassByClassId(ClassId.topLevel(topLevelFqName))

    private fun referenceClassByFqname(packageName: FqName, identifier: Name) =
        referenceClassByClassId(ClassId(packageName, identifier))

    private fun referenceClassByFqname(packageName: FqName, identifier: String) =
        referenceClassByClassId(ClassId(packageName, Name.identifier(identifier)))

    private fun referenceClassByClassId(classId: ClassId): IrClassSymbol? {
        val firSymbol = components.session.symbolProvider.getClassLikeSymbolByFqName(classId) ?: return null
        val firClassSymbol = firSymbol as? FirClassSymbol ?: return null
        return components.classifierStorage.getIrClassSymbol(firClassSymbol)
    }

    private fun referenceNestedClass(klass: IrClassSymbol, identifier: String): IrClassSymbol? =
        referenceClassByClassId(klass.owner.classId!!.createNestedClassId(Name.identifier(identifier)))

    private fun referenceNestedClass(fqName: FqName): IrClassSymbol? =
        referenceClassByClassId(ClassId(fqName.parent().parent(), fqName.parent().shortName()).createNestedClassId(fqName.shortName()))

    private fun IrType.getMaybeBuiltinClass(): IrClass? {
        val lhsClassFqName = classFqName!!
        return baseIrTypes.find { it.classFqName == lhsClassFqName }?.getClass()
            ?: referenceClassByFqname(lhsClassFqName)?.owner
    }

    private fun createPackage(fqName: FqName): IrPackageFragment =
        IrExternalPackageFragmentImpl.createEmptyExternalPackageFragment(moduleDescriptor, fqName)

    private fun IrDeclarationParent.createClass(
        fqName: FqName,
        vararg supertypes: IrType,
        classKind: ClassKind = ClassKind.CLASS,
        classModality: Modality = Modality.OPEN,
        classIsInline: Boolean = false,
        builderBlock: IrClassBuilder.() -> Unit = {},
        block: IrClass.() -> Unit = {}
    ): IrClassSymbol {
        val signature = IdSignature.PublicSignature(fqName.parent().asString(), fqName.shortName().asString(), null, 0)

        return this.createClass(
            signature, *supertypes,
            classKind = classKind, classModality = classModality, classIsInline = classIsInline, builderBlock = builderBlock, block = block
        )
    }

    private fun IrDeclarationParent.createClass(
        signature: IdSignature.PublicSignature,
        vararg supertypes: IrType,
        classKind: ClassKind = ClassKind.CLASS,
        classModality: Modality = Modality.OPEN,
        classIsInline: Boolean = false,
        builderBlock: IrClassBuilder.() -> Unit = {},
        block: IrClass.() -> Unit = {}
    ): IrClassSymbol = components.symbolTable.declareClass(
        signature,
        { IrClassPublicSymbolImpl(signature) },
        { symbol ->
            IrClassBuilder().run {
                name = Name.identifier(signature.shortName)
                kind = classKind
                modality = classModality
                isInline = classIsInline
                origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
                builderBlock()
                irFactory.createClass(
                    startOffset, endOffset, origin, symbol, name, kind, visibility, modality,
                    isCompanion, isInner, isData, isExternal, isInline, isExpect, isFun
                )
            }.also {
                it.parent = this
                it.createImplicitParameterDeclarationWithWrappedDescriptor()
                it.block()
                it.superTypes = supertypes.asList()
            }
        }
    ).symbol

    private fun IrPackageFragment.createClass(
        name: Name,
        vararg supertypes: IrType,
        classKind: ClassKind = ClassKind.CLASS,
        classModality: Modality = Modality.OPEN,
        classIsInline: Boolean = false,
        builderBlock: IrClassBuilder.() -> Unit = {},
        block: IrClass.() -> Unit = {}
    ): IrClassSymbol =
        this.createClass(
            fqName.child(name), *supertypes,
            classKind = classKind, classModality = classModality, classIsInline = classIsInline, builderBlock = builderBlock, block = block
        )

    private fun IrClass.createConstructor(
        origin: IrDeclarationOrigin = object : IrDeclarationOriginImpl("BUILTIN_CLASS_CONSTRUCTOR") {},
        isPrimary: Boolean = true,
        visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC,
        build: IrConstructor.() -> Unit = {}
    ): IrConstructorSymbol {
        val name = Name.special("<init>")
        val signature =
            IdSignature.PublicSignature(this.packageFqName!!.asString(), classId!!.relativeClassName.child(name).asString(), null, 0)
        val ctor = irFactory.createConstructor(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, IrConstructorPublicSymbolImpl(signature), name, visibility, defaultType,
            isInline = false, isExternal = false, isPrimary = isPrimary, isExpect = false
        )
        ctor.parent = this
        ctor.build()
        declarations.add(ctor)
        return ctor.symbol
    }

    private fun IrClass.forEachSuperClass(body: IrClass.() -> Unit) {
        for (st in superTypes) {
            st.getClass()?.let {
                it.body()
                it.forEachSuperClass(body)
            }
        }
    }

    private fun IrClass.createMemberFunction(
        name: String, returnType: IrType, vararg valueParameterTypes: Pair<String, IrType>,
        origin: IrDeclarationOrigin = object : IrDeclarationOriginImpl("BUILTIN_CLASS_METHOD") {},
        modality: Modality = Modality.FINAL,
        isOperator: Boolean = false,
        build: IrFunctionBuilder.() -> Unit = {}
    ) = parent.createFunction(
        IdSignature.PublicSignature(
            this.packageFqName!!.asString(),
            classId!!.relativeClassName.child(Name.identifier(name)).asString(),
            null,
            0
        ),
        name, returnType, valueParameterTypes, origin, modality, isOperator, build
    ).also { fn ->
        fn.addDispatchReceiver { type = this@createMemberFunction.defaultType }
        declarations.add(fn)
        fn.parent = this@createMemberFunction

        // very simple and fragile logic, but works for all current usages
        // TODO: replace with correct logic or explicit specification if cases become more complex
        forEachSuperClass {
            functions.find {
                it.name == fn.name && it.typeParameters.count() == fn.typeParameters.count() &&
                        it.valueParameters.count() == fn.valueParameters.count() &&
                        it.valueParameters.zip(fn.valueParameters).all { (l, r) -> l.type == r.type }
            }?.let {
                fn.overriddenSymbols += it.symbol
            }
        }
    }

    private fun IrClass.createMemberFunction(
        name: Name, returnType: IrType, vararg valueParameterTypes: Pair<String, IrType>,
        origin: IrDeclarationOrigin = object : IrDeclarationOriginImpl("BUILTIN_CLASS_METHOD") {},
        modality: Modality = Modality.FINAL,
        isOperator: Boolean = false,
        build: IrFunctionBuilder.() -> Unit = {}
    ) =
        createMemberFunction(
            name.asString(), returnType, *valueParameterTypes, origin = origin, modality = modality, isOperator = isOperator, build = build
        )

    private fun IrDeclarationParent.createFunction(
        signature: IdSignature,
        name: String,
        returnType: IrType,
        valueParameterTypes: Array<out Pair<String, IrType>>,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
        modality: Modality = Modality.FINAL,
        isOperator: Boolean = false,
        build: IrFunctionBuilder.() -> Unit = {}
    ) = IrFunctionBuilder().run {
        this.name = Name.identifier(name)
        this.returnType = returnType
        this.origin = origin
        this.modality = modality
        this.isOperator = isOperator
        build()
        irFactory.createFunction(
            startOffset, endOffset, origin, IrSimpleFunctionPublicSymbolImpl(signature), this.name, visibility, modality, this.returnType,
            isInline, isExternal, isTailrec, isSuspend, isOperator, isInfix, isExpect, isFakeOverride, containerSource,
        ).also { fn ->
            valueParameterTypes.forEachIndexed { index, (pName, irType) ->
                fn.addValueParameter(Name.identifier(if (pName.isBlank()) "arg$index" else pName), irType, origin)
            }
            fn.parent = this@createFunction
        }
    }

    private fun IrDeclarationParent.createFunction(
        packageFqName: FqName,
        name: String,
        returnType: IrType,
        valueParameterTypes: Array<out Pair<String, IrType>>,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
        modality: Modality = Modality.FINAL,
        isOperator: Boolean = false
    ) = createFunction(
        IdSignature.PublicSignature(packageFqName.asString(), name, null, 0),
        name, returnType, valueParameterTypes, origin, modality, isOperator
    )

    private fun IrClass.addArrayMembers(elementType: IrType) {
        addConstructor {
            origin = object : IrDeclarationOriginImpl("BUILTIN_CLASS_CONSTRUCTOR") {}
            returnType = defaultType
            isPrimary = true
        }.also {
            it.addValueParameter("size", intType, object : IrDeclarationOriginImpl("BUILTIN_CLASS_CONSTRUCTOR") {})
        }
        createMemberFunction(OperatorNameConventions.GET, elementType, "index" to intType, isOperator = true)
        createMemberFunction(OperatorNameConventions.SET, unitType, "index" to intType, "value" to elementType, isOperator = true)
        createProperty("size", intType)
    }

    private fun IrClass.createProperty(
        propertyName: String, returnType: IrType,
        modality: Modality = Modality.FINAL,
        isConst: Boolean = false, withGetter: Boolean = true, withField: Boolean = false, fieldInit: IrExpression? = null,
        builder: IrProperty.() -> Unit = {}
    ) {
        addProperty {
            this.name = Name.identifier(propertyName)
            this.isConst = isConst
            this.modality = modality
        }.also { property ->

            // very simple and fragile logic, but works for all current usages
            // TODO: replace with correct logic or explicit specification if cases become more complex
            forEachSuperClass {
                properties.find { it.name == property.name }?.let {
                    property.overriddenSymbols += it.symbol
                }
            }

            if (withGetter) {
                property.getter = irFactory.buildFun {
                    this.name = Name.special("<get-$propertyName>")
                    this.returnType = returnType
                    this.modality = modality
                    this.isOperator = false
                }.also {
                    it.addDispatchReceiver { type = this@createProperty.defaultType }
                    it.parent = this
                    it.correspondingPropertySymbol = property.symbol
                    it.overriddenSymbols = property.overriddenSymbols.mapNotNull { it.owner.getter?.symbol }
                }
            }
            if (withField || fieldInit != null) {
                property.backingField = irFactory.buildField {
                    this.name = property.name
                    this.type = defaultType
                }.also {
                    if (fieldInit != null) {
                        it.initializer = irFactory.createExpressionBody(0, 0) {
                            expression = fieldInit
                        }
                    }
                    it.correspondingPropertySymbol = property.symbol
                }
            }
            property.builder()
        }
    }

    private class NumericConstantsExpressions<T>(
        val min: IrConst<T>,
        val max: IrConst<T>,
        val sizeBytes: IrConst<Int>,
        val sizeBits: IrConst<Int>
    )

    private fun getNumericConstantsExpressions(type: IrType): NumericConstantsExpressions<*> {
        val so = UNDEFINED_OFFSET
        val eo = UNDEFINED_OFFSET
        return when (type.getPrimitiveType()) {
            PrimitiveType.CHAR -> NumericConstantsExpressions(
                IrConstImpl.char(so, eo, type, Char.MIN_VALUE), IrConstImpl.char(so, eo, type, Char.MAX_VALUE),
                IrConstImpl.int(so, eo, intType, Char.SIZE_BYTES), IrConstImpl.int(so, eo, intType, Char.SIZE_BITS)
            )
            PrimitiveType.BYTE -> NumericConstantsExpressions(
                IrConstImpl.byte(so, eo, type, Byte.MIN_VALUE), IrConstImpl.byte(so, eo, type, Byte.MAX_VALUE),
                IrConstImpl.int(so, eo, intType, Byte.SIZE_BYTES), IrConstImpl.int(so, eo, intType, Byte.SIZE_BITS)
            )
            PrimitiveType.SHORT -> NumericConstantsExpressions(
                IrConstImpl.short(so, eo, type, Short.MIN_VALUE), IrConstImpl.short(so, eo, type, Short.MAX_VALUE),
                IrConstImpl.int(so, eo, intType, Short.SIZE_BYTES), IrConstImpl.int(so, eo, intType, Short.SIZE_BITS)
            )
            PrimitiveType.INT -> NumericConstantsExpressions(
                IrConstImpl.int(so, eo, type, Int.MIN_VALUE), IrConstImpl.int(so, eo, type, Int.MAX_VALUE),
                IrConstImpl.int(so, eo, intType, Int.SIZE_BYTES), IrConstImpl.int(so, eo, intType, Int.SIZE_BITS)
            )
            PrimitiveType.LONG -> NumericConstantsExpressions(
                IrConstImpl.long(so, eo, type, Long.MIN_VALUE), IrConstImpl.long(so, eo, type, Long.MAX_VALUE),
                IrConstImpl.int(so, eo, intType, Long.SIZE_BYTES), IrConstImpl.int(so, eo, intType, Long.SIZE_BITS)
            )
            PrimitiveType.FLOAT -> NumericConstantsExpressions(
                IrConstImpl.float(so, eo, type, Float.MIN_VALUE), IrConstImpl.float(so, eo, type, Float.MAX_VALUE),
                IrConstImpl.int(so, eo, intType, Float.SIZE_BYTES), IrConstImpl.int(so, eo, intType, Float.SIZE_BITS)
            )
            PrimitiveType.DOUBLE -> NumericConstantsExpressions(
                IrConstImpl.double(so, eo, type, Double.MIN_VALUE), IrConstImpl.double(so, eo, type, Double.MAX_VALUE),
                IrConstImpl.int(so, eo, intType, Double.SIZE_BYTES), IrConstImpl.int(so, eo, intType, Double.SIZE_BITS)
            )
            else -> error("unsupported type")
        }
    }

    private fun IrPackageFragment.createNumberClass(signature: IdSignature.PublicSignature, builder: IrClass.() -> Unit = {}): IrClassSymbol =
        createClass(signature, numberType) {
            builder()
        }

    private fun IrClass.createCompanionObject(block: IrClass.() -> Unit = {}): IrClassSymbol =
        this.createClass(
            kotlinFqName.child(Name.identifier("Companion")), classKind = ClassKind.OBJECT, builderBlock = {
                isCompanion = true
            }
        ).also {
            it.owner.block()
            declarations.add(it.owner)
        }

    private fun findFunctions(packageName: FqName, name: Name) =
        components.session.symbolProvider.getTopLevelFunctionSymbols(packageName, name).mapNotNull { firOpSymbol ->
            components.declarationStorage.getIrFunctionSymbol(firOpSymbol) as? IrSimpleFunctionSymbol
        }
}
