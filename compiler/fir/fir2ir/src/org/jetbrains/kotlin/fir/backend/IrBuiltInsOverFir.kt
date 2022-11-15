/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.backend.common.ir.addDispatchReceiver
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.ir.BuiltInOperatorNames
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions
import kotlin.reflect.KProperty

class IrBuiltInsOverFir(
    private val components: Fir2IrComponents,
    override val languageVersionSettings: LanguageVersionSettings,
    private val moduleDescriptor: FirModuleDescriptor,
    irMangler: KotlinMangler.IrMangler,
    private val tryLoadBuiltInsFirst: Boolean = false
) : IrBuiltIns() {

    override val irFactory: IrFactory = components.symbolTable.irFactory

    private val kotlinPackage = StandardClassIds.BASE_KOTLIN_PACKAGE
    private val kotlinInternalPackage = StandardClassIds.BASE_INTERNAL_PACKAGE

    override val operatorsPackageFragment = createPackage(KOTLIN_INTERNAL_IR_FQN)
    private val kotlinIrPackage = createPackage(kotlinPackage)
    private val kotlinInternalIrPackage = createPackage(kotlinInternalPackage)

    private val irSignatureBuilder = PublicIdSignatureComputer(irMangler)

    override val booleanNotSymbol: IrSimpleFunctionSymbol by lazy {
        boolean.ensureLazyContentsCreated()
        booleanClass.owner.functions.first { it.name == OperatorNameConventions.NOT && it.returnType == booleanType }.symbol
    }

    private val any by createClass(kotlinIrPackage, IdSignatureValues.any, build = { modality = Modality.OPEN }) {
        createConstructor()
        createMemberFunction(
            OperatorNameConventions.EQUALS, booleanType, "other" to anyNType,
            modality = Modality.OPEN, isOperator = true, isIntrinsicConst = false
        )
        createMemberFunction("hashCode", intType, modality = Modality.OPEN, isIntrinsicConst = false)
        createMemberFunction("toString", stringType, modality = Modality.OPEN, isIntrinsicConst = false)
    }
    override val anyClass: IrClassSymbol get() = any.klass
    override val anyType: IrType get() = any.type
    override val anyNType by lazy { anyType.makeNullable() }

    private val number by createClass(kotlinIrPackage, IdSignatureValues.number, build = { modality = Modality.ABSTRACT }) {
        configureSuperTypes()
        for (targetPrimitive in primitiveIrTypesWithComparisons) {
            createMemberFunction("to${targetPrimitive.classFqName!!.shortName().asString()}", targetPrimitive, modality = Modality.ABSTRACT)
        }
        finalizeClassDefinition()
    }
    override val numberClass: IrClassSymbol get() = number.klass
    override val numberType: IrType get() = number.type

    private val nothing by createClass(kotlinIrPackage, IdSignatureValues.nothing)
    override val nothingClass: IrClassSymbol get() = nothing.klass
    override val nothingType: IrType get() = nothing.type
    override val nothingNType: IrType by lazy { nothingType.makeNullable() }

    private val unit by createClass(kotlinIrPackage, IdSignatureValues.unit, build = { kind = ClassKind.OBJECT; modality = Modality.FINAL })
    override val unitClass: IrClassSymbol get() = unit.klass
    override val unitType: IrType get() = unit.type

    private val boolean by createClass(kotlinIrPackage, IdSignatureValues._boolean) {
        configureSuperTypes()
        // TODO: dangerous dependency on call sequence, consider making extended BuiltInsClass to trigger lazy initialization
        createMemberFunction(OperatorNameConventions.NOT, booleanType, isOperator = true).symbol
        createMemberFunction(OperatorNameConventions.AND, booleanType, "other" to booleanType) { isInfix = true }
        createMemberFunction(OperatorNameConventions.OR, booleanType, "other" to booleanType) { isInfix = true }
        createMemberFunction(OperatorNameConventions.XOR, booleanType, "other" to booleanType) { isInfix = true }
        createMemberFunction(
            OperatorNameConventions.COMPARE_TO,
            intType,
            "other" to booleanType,
            modality = Modality.OPEN,
            isOperator = true
        )
        createIntrinsicConstOfToStringAndEquals()
        finalizeClassDefinition()
    }
    override val booleanType: IrType get() = boolean.type
    override val booleanClass: IrClassSymbol get() = boolean.klass

    private val char by createClass(kotlinIrPackage, IdSignatureValues._char) {
        configureSuperTypes(number)
        createStandardNumericAndCharMembers(charType)
        createMemberFunction(OperatorNameConventions.COMPARE_TO, intType, "other" to charType, modality = Modality.OPEN, isOperator = true)
        createMemberFunction(OperatorNameConventions.PLUS, charType, "other" to intType, isOperator = true)
        createMemberFunction(OperatorNameConventions.MINUS, charType, "other" to intType, isOperator = true)
        createMemberFunction(OperatorNameConventions.MINUS, intType, "other" to charType, isOperator = true)
        val charRange = referenceClassByClassId(StandardClassIds.CharRange)!!.owner.defaultType
        createMemberFunction(OperatorNameConventions.RANGE_TO, charRange, "other" to charType, isIntrinsicConst = false)
        createMemberFunction(OperatorNameConventions.RANGE_UNTIL, charRange, "other" to charType, isIntrinsicConst = false)
        createIntrinsicConstOfToStringAndEquals()
        finalizeClassDefinition()
    }
    override val charClass: IrClassSymbol get() = char.klass
    override val charType: IrType get() = char.type

    private val byte by kotlinIrPackage.createNumberClass(IdSignatureValues._byte)
    override val byteType: IrType get() = byte.type
    override val byteClass: IrClassSymbol get() = byte.klass

    private val short by kotlinIrPackage.createNumberClass(IdSignatureValues._short)
    override val shortType: IrType get() = short.type
    override val shortClass: IrClassSymbol get() = short.klass

    private val int by kotlinIrPackage.createNumberClass(IdSignatureValues._int)
    override val intType: IrType get() = int.type
    override val intClass: IrClassSymbol get() = int.klass

    private val long by kotlinIrPackage.createNumberClass(IdSignatureValues._long)
    override val longType: IrType get() = long.type
    override val longClass: IrClassSymbol get() = long.klass

    private val float by kotlinIrPackage.createNumberClass(IdSignatureValues._float)
    override val floatType: IrType get() = float.type
    override val floatClass: IrClassSymbol get() = float.klass

    private val double by kotlinIrPackage.createNumberClass(IdSignatureValues._double)
    override val doubleType: IrType get() = double.type
    override val doubleClass: IrClassSymbol get() = double.klass

    private val charSequence by createClass(
        kotlinIrPackage, IdSignatureValues.charSequence,
        build = { kind = ClassKind.INTERFACE; modality = Modality.OPEN }
    ) {
        configureSuperTypes()
        createProperty("length", intType, modality = Modality.ABSTRACT)
        createMemberFunction(OperatorNameConventions.GET, charType, "index" to intType, modality = Modality.ABSTRACT, isOperator = true, isIntrinsicConst = false)
        createMemberFunction("subSequence", defaultType, "startIndex" to intType, "endIndex" to intType, modality = Modality.ABSTRACT, isIntrinsicConst = false)
        finalizeClassDefinition()
    }
    override val charSequenceClass: IrClassSymbol get() = charSequence.klass

    private val string by createClass(kotlinIrPackage, IdSignatureValues.string) {
        configureSuperTypes(charSequence)
        createProperty("length", intType, modality = Modality.OPEN, isIntrinsicConst = true)
        createMemberFunction(OperatorNameConventions.GET, charType, "index" to intType, modality = Modality.OPEN, isOperator = true)
        createMemberFunction(
            "subSequence",
            charSequenceClass.defaultType,
            "startIndex" to intType,
            "endIndex" to intType,
            modality = Modality.OPEN,
            isIntrinsicConst = false
        )
        createMemberFunction(
            OperatorNameConventions.COMPARE_TO,
            intType,
            "other" to defaultType,
            modality = Modality.OPEN,
            isOperator = true
        )
        createMemberFunction(OperatorNameConventions.PLUS, defaultType, "other" to anyNType, isOperator = true)
        createIntrinsicConstOfToStringAndEquals()
        finalizeClassDefinition()
    }
    override val stringClass: IrClassSymbol get() = string.klass
    override val stringType: IrType get() = string.type

    private val intrinsicConstAnnotationFqName = kotlinInternalPackage.child(Name.identifier("IntrinsicConstEvaluation"))
    internal val intrinsicConst = kotlinInternalIrPackage.createClass(intrinsicConstAnnotationFqName).apply {
        owner.createConstructor()
        owner.finalizeClassDefinition()
    }

    private val intrinsicConstAnnotation: IrConstructorCall = run {
        val constructor = intrinsicConst.constructors.single()
        IrConstructorCallImpl.Companion.fromSymbolOwner(intrinsicConst.defaultType, constructor)
    }

    private val iterator by loadClass(StandardClassIds.Iterator)
    override val iteratorClass: IrClassSymbol get() = iterator.klass

    private val array by createClass(kotlinIrPackage, IdSignatureValues.array) {
        configureSuperTypes()
        val typeParameter = addTypeParameter("T", anyNType)
        addArrayMembers(typeParameter.defaultType, iteratorClass.typeWith(typeParameter.defaultType))
        finalizeClassDefinition()
    }
    override val arrayClass: IrClassSymbol get() = array.klass

    private val intRangeType by lazy { referenceClassByClassId(StandardClassIds.IntRange)!!.owner.defaultType }
    private val longRangeType by lazy { referenceClassByClassId(StandardClassIds.LongRange)!!.owner.defaultType }

    private val annotation by loadClass(StandardClassIds.Annotation)
    override val annotationClass: IrClassSymbol get() = annotation.klass
    override val annotationType: IrType get() = annotation.type

    private val collection by loadClass(StandardClassIds.Collection)
    override val collectionClass: IrClassSymbol get() = collection.klass
    private val set by loadClass(StandardClassIds.Set)
    override val setClass: IrClassSymbol get() = set.klass
    private val list by loadClass(StandardClassIds.List)
    override val listClass: IrClassSymbol get() = list.klass
    private val map by loadClass(StandardClassIds.Map)
    override val mapClass: IrClassSymbol get() = map.klass
    private val mapEntry by BuiltInsClass({ true to referenceClassByClassId(StandardClassIds.MapEntry)!! })
    override val mapEntryClass: IrClassSymbol get() = mapEntry.klass

    private val iterable by loadClass(StandardClassIds.Iterable)
    override val iterableClass: IrClassSymbol get() = iterable.klass
    private val listIterator by loadClass(StandardClassIds.ListIterator)
    override val listIteratorClass: IrClassSymbol get() = listIterator.klass
    private val mutableCollection by loadClass(StandardClassIds.MutableCollection)
    override val mutableCollectionClass: IrClassSymbol get() = mutableCollection.klass
    private val mutableSet by loadClass(StandardClassIds.MutableSet)
    override val mutableSetClass: IrClassSymbol get() = mutableSet.klass
    private val mutableList by loadClass(StandardClassIds.MutableList)
    override val mutableListClass: IrClassSymbol get() = mutableList.klass
    private val mutableMap by loadClass(StandardClassIds.MutableMap)
    override val mutableMapClass: IrClassSymbol get() = mutableMap.klass
    private val mutableMapEntry by BuiltInsClass({ true to referenceClassByClassId(StandardClassIds.MutableMapEntry)!! })
    override val mutableMapEntryClass: IrClassSymbol get() = mutableMapEntry.klass

    private val mutableIterable by loadClass(StandardClassIds.MutableIterable)
    override val mutableIterableClass: IrClassSymbol get() = mutableIterable.klass
    private val mutableIterator by loadClass(StandardClassIds.MutableIterator)
    override val mutableIteratorClass: IrClassSymbol get() = mutableIterator.klass
    private val mutableListIterator by loadClass(StandardClassIds.MutableListIterator)
    override val mutableListIteratorClass: IrClassSymbol get() = mutableListIterator.klass
    private val comparable by loadClass(StandardClassIds.Comparable)
    override val comparableClass: IrClassSymbol get() = comparable.klass
    override val throwableType: IrType by lazy { throwableClass.defaultType }
    private val throwable by loadClass(StandardClassIds.Throwable)
    override val throwableClass: IrClassSymbol get() = throwable.klass

    private val kCallable by loadClass(StandardClassIds.KCallable)
    override val kCallableClass: IrClassSymbol get() = kCallable.klass
    private val kProperty by loadClass(StandardClassIds.KProperty)
    override val kPropertyClass: IrClassSymbol get() = kProperty.klass
    private val kClass by loadClass(StandardClassIds.KClass)
    override val kClassClass: IrClassSymbol get() = kClass.klass
    private val kProperty0 by loadClass(StandardClassIds.KProperty0)
    override val kProperty0Class: IrClassSymbol get() = kProperty0.klass
    private val kProperty1 by loadClass(StandardClassIds.KProperty1)
    override val kProperty1Class: IrClassSymbol get() = kProperty1.klass
    private val kProperty2 by loadClass(StandardClassIds.KProperty2)
    override val kProperty2Class: IrClassSymbol get() = kProperty2.klass
    private val kMutableProperty0 by loadClass(StandardClassIds.KMutableProperty0)
    override val kMutableProperty0Class: IrClassSymbol get() = kMutableProperty0.klass
    private val kMutableProperty1 by loadClass(StandardClassIds.KMutableProperty1)
    override val kMutableProperty1Class: IrClassSymbol get() = kMutableProperty1.klass
    private val kMutableProperty2 by loadClass(StandardClassIds.KMutableProperty2)
    override val kMutableProperty2Class: IrClassSymbol get() = kMutableProperty2.klass

    private val function by loadClass(StandardClassIds.Function)
    override val functionClass: IrClassSymbol get() = function.klass
    private val kFunction by loadClass(StandardClassIds.KFunction)
    override val kFunctionClass: IrClassSymbol get() = kFunction.klass

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

    private val bitwiseOperators = arrayOf(OperatorNameConventions.AND, OperatorNameConventions.OR, OperatorNameConventions.XOR)
    private val shiftOperators = arrayOf(OperatorNameConventions.SHL, OperatorNameConventions.SHR, OperatorNameConventions.USHR)
    private val arithmeticOperators = arrayOf(
        OperatorNameConventions.PLUS,
        OperatorNameConventions.MINUS,
        OperatorNameConventions.TIMES,
        OperatorNameConventions.DIV,
        OperatorNameConventions.REM
    )

    private fun getPrimitiveArithmeticOperatorResultType(target: IrType, arg: IrType) =
        when {
            arg == doubleType -> arg
            target in primitiveFloatingPointIrTypes -> target
            arg in primitiveFloatingPointIrTypes -> arg
            target == longType -> target
            arg == longType -> arg
            else -> intType
        }

    private fun primitiveIterator(primitiveType: PrimitiveType) =
        loadClass(ClassId(StandardClassIds.BASE_COLLECTIONS_PACKAGE, Name.identifier("${primitiveType.typeName}Iterator")))

    private val booleanIterator by primitiveIterator(PrimitiveType.BOOLEAN)
    private val charIterator by primitiveIterator(PrimitiveType.CHAR)
    private val byteIterator by primitiveIterator(PrimitiveType.BYTE)
    private val shortIterator by primitiveIterator(PrimitiveType.SHORT)
    private val intIterator by primitiveIterator(PrimitiveType.INT)
    private val longIterator by primitiveIterator(PrimitiveType.LONG)
    private val floatIterator by primitiveIterator(PrimitiveType.FLOAT)
    private val doubleIterator by primitiveIterator(PrimitiveType.DOUBLE)

    private val _booleanArray by createPrimitiveArrayClass(kotlinIrPackage, PrimitiveType.BOOLEAN, booleanIterator)
    private val _charArray by createPrimitiveArrayClass(kotlinIrPackage, PrimitiveType.CHAR, charIterator)
    private val _byteArray by createPrimitiveArrayClass(kotlinIrPackage, PrimitiveType.BYTE, byteIterator)
    private val _shortArray by createPrimitiveArrayClass(kotlinIrPackage, PrimitiveType.SHORT, shortIterator)
    private val _intArray by createPrimitiveArrayClass(kotlinIrPackage, PrimitiveType.INT, intIterator)
    private val _longArray by createPrimitiveArrayClass(kotlinIrPackage, PrimitiveType.LONG, longIterator)
    private val _floatArray by createPrimitiveArrayClass(kotlinIrPackage, PrimitiveType.FLOAT, floatIterator)
    private val _doubleArray by createPrimitiveArrayClass(kotlinIrPackage, PrimitiveType.DOUBLE, doubleIterator)

    override val booleanArray: IrClassSymbol get() = _booleanArray.klass
    override val charArray: IrClassSymbol get() = _charArray.klass
    override val byteArray: IrClassSymbol get() = _byteArray.klass
    override val shortArray: IrClassSymbol get() = _shortArray.klass
    override val intArray: IrClassSymbol get() = _intArray.klass
    override val longArray: IrClassSymbol get() = _longArray.klass
    override val floatArray: IrClassSymbol get() = _floatArray.klass
    override val doubleArray: IrClassSymbol get() = _doubleArray.klass

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

    override val linkageErrorSymbol: IrSimpleFunctionSymbol
        get() = TODO("Not yet implemented")

    override lateinit var lessFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> private set
    override lateinit var lessOrEqualFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> private set
    override lateinit var greaterOrEqualFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> private set
    override lateinit var greaterFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> private set

    init {
        with(this.operatorsPackageFragment) {

            fun addBuiltinOperatorSymbol(
                name: String,
                returnType: IrType,
                vararg valueParameterTypes: Pair<String, IrType>,
                isIntrinsicConst: Boolean = false
            ) =
                createFunction(name, returnType, valueParameterTypes, origin = BUILTIN_OPERATOR, isIntrinsicConst = isIntrinsicConst).also {
                    declarations.add(it)
                }.symbol

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
                addBuiltinOperatorSymbol(BuiltInOperatorNames.ANDAND, booleanType, "" to booleanType, "" to booleanType, isIntrinsicConst = true)
            ororSymbol =
                addBuiltinOperatorSymbol(BuiltInOperatorNames.OROR, booleanType, "" to booleanType, "" to booleanType, isIntrinsicConst = true)
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
                    superTypes = listOf(anyType)
                }

                createFunction(
                    "CHECK_NOT_NULL",
                    IrSimpleTypeImpl(typeParameter.symbol, SimpleTypeNullability.DEFINITELY_NOT_NULL, emptyList(), emptyList()),
                    arrayOf("" to IrSimpleTypeImpl(typeParameter.symbol, hasQuestionMark = true, emptyList(), emptyList())),
                    typeParameters = listOf(typeParameter),
                    origin = BUILTIN_OPERATOR
                ).also {
                    declarations.add(it)
                }.symbol
            }

            fun List<IrType>.defineComparisonOperatorForEachIrType(name: String) =
                associate { it.classifierOrFail to addBuiltinOperatorSymbol(name, booleanType, "" to it, "" to it, isIntrinsicConst = true) }

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

    override val unsignedArraysElementTypes: Map<IrClassSymbol, IrType?> by lazy {
        unsignedTypesToUnsignedArrays.map { (k,v) -> v to referenceClassByClassId(k.classId)?.owner?.defaultType }.toMap()
    }

    override fun getKPropertyClass(mutable: Boolean, n: Int): IrClassSymbol = when (n) {
        0 -> if (mutable) kMutableProperty0Class else kProperty0Class
        1 -> if (mutable) kMutableProperty1Class else kProperty1Class
        2 -> if (mutable) kMutableProperty2Class else kProperty2Class
        else -> error("No KProperty for n=$n mutable=$mutable")
    }

    private val enum by loadClass(StandardClassIds.Enum)
    override val enumClass: IrClassSymbol get() = enum.klass

    override val intPlusSymbol: IrSimpleFunctionSymbol
        get() = intClass.functions.single {
            it.owner.name == OperatorNameConventions.PLUS && it.owner.valueParameters[0].type == intType
        }

    override val intTimesSymbol: IrSimpleFunctionSymbol
        get() = intClass.functions.single {
            it.owner.name == OperatorNameConventions.TIMES && it.owner.valueParameters[0].type == intType
        }

    override val intXorSymbol: IrSimpleFunctionSymbol
        get() = intClass.functions.single {
            it.owner.name == OperatorNameConventions.XOR && it.owner.valueParameters[0].type == intType
        }

    override val extensionToString: IrSimpleFunctionSymbol by lazy {
        findFunctions(kotlinPackage, OperatorNameConventions.TO_STRING).single { function ->
            function.owner.extensionReceiverParameter?.let { receiver -> receiver.type == anyNType } ?: false
        }
    }

    override val memberToString: IrSimpleFunctionSymbol by lazy {
        findBuiltInClassMemberFunctions(anyClass, OperatorNameConventions.TO_STRING).single { function ->
            function.owner.valueParameters.isEmpty()
        }
    }

    override val extensionStringPlus: IrSimpleFunctionSymbol by lazy {
        findFunctions(kotlinPackage, OperatorNameConventions.PLUS).single { function ->
            val isStringExtension =
                function.owner.extensionReceiverParameter?.let { receiver -> receiver.type == stringType.makeNullable() }
                    ?: false
            isStringExtension && function.owner.valueParameters.size == 1 && function.owner.valueParameters[0].type == anyNType
        }
    }

    override val memberStringPlus: IrSimpleFunctionSymbol by lazy {
        findBuiltInClassMemberFunctions(stringClass, OperatorNameConventions.PLUS).single { function ->
            function.owner.valueParameters.size == 1 && function.owner.valueParameters[0].type == anyNType
        }
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
            kotlinIrPackage.createFunction(name, returnType, argumentTypes, postBuild = builder).also {
                this.owner.declarations.add(it)
            }.symbol

        val kotlinKt = kotlinIrPackage.createClass(kotlinPackage.child(Name.identifier("KotlinKt")))
        KotlinPackageFuns(
            arrayOf = kotlinKt.addPackageFun("arrayOf", arrayClass.defaultType) arrayOf@{
                val typeParameter = addTypeParameter("T", anyNType)
                addValueParameter {
                    this.name = Name.identifier("elements")
                    this.type = arrayClass.typeWithParameters(listOf(typeParameter))
                    this.varargElementType = typeParameter.defaultType
                    this.origin = this@arrayOf.origin
                }
                returnType = arrayClass.typeWithParameters(listOf(typeParameter))
            }
        )
    }

    override val arrayOf: IrSimpleFunctionSymbol get() = kotlinBuiltinFunctions.arrayOf

    private fun <T : Any> getFunctionsByKey(
        name: Name,
        vararg packageNameSegments: String,
        makeKey: (IrSimpleFunctionSymbol) -> T?
    ): Map<T, IrSimpleFunctionSymbol> {
        val result = mutableMapOf<T, IrSimpleFunctionSymbol>()
        for (fn in findFunctions(name, *packageNameSegments)) {
            makeKey(fn)?.let { key ->
                result[key] = fn
            }
        }
        return result
    }

    override fun getNonBuiltInFunctionsByExtensionReceiver(
        name: Name, vararg packageNameSegments: String
    ): Map<IrClassifierSymbol, IrSimpleFunctionSymbol> =
        getFunctionsByKey(name, *packageNameSegments) { fn ->
            fn.owner.extensionReceiverParameter?.type?.classifierOrNull
        }

    override fun getNonBuiltinFunctionsByReturnType(
        name: Name, vararg packageNameSegments: String
    ): Map<IrClassifierSymbol, IrSimpleFunctionSymbol> =
        getFunctionsByKey(name, *packageNameSegments) { fn ->
            fn.owner.returnType.classOrNull
        }

    private val functionNMap = mutableMapOf<Int, IrClass>()
    private val kFunctionNMap = mutableMapOf<Int, IrClass>()
    private val suspendFunctionNMap = mutableMapOf<Int, IrClass>()
    private val kSuspendFunctionNMap = mutableMapOf<Int, IrClass>()

    override fun functionN(arity: Int): IrClass = functionNMap.getOrPut(arity) {
        referenceClassByClassId(StandardClassIds.FunctionN(arity))!!.owner
    }

    override fun kFunctionN(arity: Int): IrClass = kFunctionNMap.getOrPut(arity) {
        referenceClassByClassId(StandardClassIds.KFunctionN(arity))!!.owner
    }

    override fun suspendFunctionN(arity: Int): IrClass = suspendFunctionNMap.getOrPut(arity) {
        referenceClassByClassId(StandardClassIds.SuspendFunctionN(arity))!!.owner
    }

    override fun kSuspendFunctionN(arity: Int): IrClass = kSuspendFunctionNMap.getOrPut(arity) {
        referenceClassByClassId(StandardClassIds.KSuspendFunctionN(arity))!!.owner
    }

    override fun findFunctions(name: Name, vararg packageNameSegments: String): Iterable<IrSimpleFunctionSymbol> =
        findFunctions(FqName.fromSegments(packageNameSegments.asList()), name)

    override fun findFunctions(name: Name, packageFqName: FqName): Iterable<IrSimpleFunctionSymbol> =
        findFunctions(packageFqName, name)

    override fun findClass(name: Name, vararg packageNameSegments: String): IrClassSymbol? =
        referenceClassByFqname(FqName.fromSegments(packageNameSegments.asList()), name)

    override fun findClass(name: Name, packageFqName: FqName): IrClassSymbol? =
        referenceClassByFqname(packageFqName, name)

    private fun referenceClassByFqname(packageName: FqName, identifier: Name) =
        referenceClassByClassId(ClassId(packageName, identifier))

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

// ---------------

    class BuiltInClassValue(
        private val generatedClass: IrClassSymbol,
        private var lazyContents: (IrClass.() -> Unit)?
    ) {
        fun ensureLazyContentsCreated() {
            if (lazyContents != null) synchronized(this) {
                lazyContents?.invoke(generatedClass.owner)
                lazyContents = null
            }
        }

        val klass: IrClassSymbol
            get() {
                ensureLazyContentsCreated()
                return generatedClass
            }

        val type: IrType get() = generatedClass.defaultType
    }

    private inner class BuiltInsClass(
        private var generator: (() -> Pair<Boolean, IrClassSymbol>)?,
        private var lazyContents: (IrClass.() -> Unit)? = null
    ) {

        private var value: BuiltInClassValue? = null

        operator fun getValue(thisRef: Any?, property: KProperty<*>): BuiltInClassValue = value ?: run {
            synchronized(this) {
                if (value == null) {
                    val (isLoaded, symbol) = generator!!()
                    value = BuiltInClassValue(symbol, if (isLoaded) null else lazyContents)
                    generator = null
                    lazyContents = null
                }
            }
            value!!
        }
    }

    private fun loadClass(classId: ClassId) = BuiltInsClass({ true to referenceClassByClassId(classId)!! })

    private fun createClass(
        parent: IrDeclarationParent,
        signature: IdSignature.CommonSignature,
        build: IrClassBuilder.() -> Unit = {},
        lazyContents: (IrClass.() -> Unit) = { finalizeClassDefinition() }
    ) = BuiltInsClass(
        generator = {
            val loaded = if (tryLoadBuiltInsFirst) {
                referenceClassByClassId(ClassId(parent.kotlinFqName, Name.identifier(signature.shortName)))
            } else null
            (loaded != null) to (loaded ?: components.symbolTable.declareClass(
                signature,
                { IrClassPublicSymbolImpl(signature) },
                { symbol ->
                    IrClassBuilder().run {
                        name = Name.identifier(signature.shortName)
                        origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
                        build()
                        irFactory.createClass(
                            startOffset, endOffset, origin, symbol, name, kind, visibility, modality,
                            isCompanion, isInner, isData, isExternal, isValue, isExpect, isFun
                        )
                    }.also {
                        it.parent = parent
                        it.createImplicitParameterDeclarationWithWrappedDescriptor()
                        components.symbolTable.declareClassWithSignature(irSignatureBuilder.computeSignature(it), it.symbol)
                    }
                }
            ).symbol)
        },
        lazyContents = lazyContents
    )

    private fun referenceClassByFqname(topLevelFqName: FqName) =
        referenceClassByClassId(ClassId.topLevel(topLevelFqName))

    private fun referenceClassByClassId(classId: ClassId): IrClassSymbol? {
        val firSymbol = components.session.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
        val firClassSymbol = firSymbol as? FirClassSymbol ?: return null
        return components.classifierStorage.getIrClassSymbol(firClassSymbol)
    }

    private fun IrType.getMaybeBuiltinClass(): IrClass? {
        val lhsClassFqName = classFqName!!
        return baseIrTypes.find { it.classFqName == lhsClassFqName }?.getClass()
            ?: referenceClassByFqname(lhsClassFqName)?.owner
    }

    private fun createPackage(fqName: FqName): IrExternalPackageFragment =
        IrExternalPackageFragmentImpl.createEmptyExternalPackageFragment(moduleDescriptor, fqName)

    private fun IrDeclarationParent.createClass(
        fqName: FqName,
        vararg supertypes: IrType,
        classKind: ClassKind = ClassKind.CLASS,
        classModality: Modality = Modality.OPEN,
        builderBlock: IrClassBuilder.() -> Unit = {},
        block: IrClass.() -> Unit = {}
    ): IrClassSymbol {
        val signature = getPublicSignature(fqName.parent(), fqName.shortName().asString())

        return this.createClass(
            signature, *supertypes,
            classKind = classKind, classModality = classModality, builderBlock = builderBlock, block = block
        )
    }

    private fun IrDeclarationParent.createClass(
        signature: IdSignature.CommonSignature,
        vararg supertypes: IrType,
        classKind: ClassKind = ClassKind.CLASS,
        classModality: Modality = Modality.OPEN,
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
                origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
                builderBlock()
                irFactory.createClass(
                    startOffset, endOffset, origin, symbol, name, kind, visibility, modality,
                    isCompanion, isInner, isData, isExternal, isValue, isExpect, isFun
                )
            }.also {
                it.parent = this
                it.createImplicitParameterDeclarationWithWrappedDescriptor()
                it.block()
                it.superTypes = supertypes.asList()
            }
        }
    ).symbol

    private fun IrClass.createConstructor(
        origin: IrDeclarationOrigin = object : IrDeclarationOriginImpl("BUILTIN_CLASS_CONSTRUCTOR") {},
        isPrimary: Boolean = true,
        visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC,
        build: IrConstructor.() -> Unit = {}
    ): IrConstructorSymbol {
        val name = SpecialNames.INIT
        val ctor = irFactory.createConstructor(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, IrConstructorSymbolImpl(), name, visibility, defaultType,
            isInline = false, isExternal = false, isPrimary = isPrimary, isExpect = false
        )
        ctor.parent = this
        ctor.build()
        declarations.add(ctor)
        components.symbolTable.declareConstructorWithSignature(
            irSignatureBuilder.computeSignature(ctor), ctor.symbol
        )
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
        isInfix: Boolean = false,
        isIntrinsicConst: Boolean = true,
        build: IrFunctionBuilder.() -> Unit = {}
    ) = createFunction(
        name, returnType, valueParameterTypes,
        origin = origin, modality = modality, isOperator = isOperator, isInfix = isInfix, isIntrinsicConst = isIntrinsicConst,
        postBuild = {
            addDispatchReceiver { type = this@createMemberFunction.defaultType }
        },
        build = build
    ).also { fn ->
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

        declarations.add(fn)
    }

    private fun IrClass.createMemberFunction(
        name: Name, returnType: IrType, vararg valueParameterTypes: Pair<String, IrType>,
        origin: IrDeclarationOrigin = object : IrDeclarationOriginImpl("BUILTIN_CLASS_METHOD") {},
        modality: Modality = Modality.FINAL,
        isOperator: Boolean = false,
        isInfix: Boolean = false,
        isIntrinsicConst: Boolean = true,
        build: IrFunctionBuilder.() -> Unit = {}
    ) =
        createMemberFunction(
            name.asString(), returnType, *valueParameterTypes,
            origin = origin, modality = modality, isOperator = isOperator, isInfix = isInfix,
            isIntrinsicConst = isIntrinsicConst, build = build
        )

    private fun IrClass.configureSuperTypes(vararg superTypes: BuiltInClassValue, defaultAny: Boolean = true) {
        for (superType in superTypes) {
            superType.ensureLazyContentsCreated()
        }
        if (!defaultAny || superTypes.contains(any) || this.superTypes.contains(anyType)) {
            this.superTypes += superTypes.map { it.type }
        } else {
            any.ensureLazyContentsCreated()
            this.superTypes += superTypes.map { it.type } + anyType
        }
    }

    private fun IrClass.finalizeClassDefinition() {
        addFakeOverrides(IrTypeSystemContextImpl(this@IrBuiltInsOverFir))
    }

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
            irFactory.createFunction(
                startOffset, endOffset, this.origin,
                symbol,
                this.name, visibility, this.modality, this.returnType,
                isInline, isExternal, isTailrec, isSuspend, this.isOperator, this.isInfix, isExpect, isFakeOverride,
                containerSource,
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
        return components.symbolTable.declareSimpleFunction(signature, { IrSimpleFunctionPublicSymbolImpl(signature, null) }, ::makeWithSymbol)
    }

    private fun IrClass.addArrayMembers(elementType: IrType, iteratorType: IrType) {
        addConstructor {
            origin = object : IrDeclarationOriginImpl("BUILTIN_CLASS_CONSTRUCTOR") {}
            returnType = defaultType
            isPrimary = true
        }.also {
            it.addValueParameter("size", intType, object : IrDeclarationOriginImpl("BUILTIN_CLASS_CONSTRUCTOR") {})
        }
        createMemberFunction(OperatorNameConventions.GET, elementType, "index" to intType, isOperator = true, isIntrinsicConst = false)
        createMemberFunction(OperatorNameConventions.SET, unitType, "index" to intType, "value" to elementType, isOperator = true, isIntrinsicConst = false)
        createProperty("size", intType)
        createMemberFunction(OperatorNameConventions.ITERATOR, iteratorType, isOperator = true)
    }

    private fun IrClass.createProperty(
        propertyName: String, returnType: IrType,
        modality: Modality = Modality.FINAL,
        isConst: Boolean = false, withGetter: Boolean = true, withField: Boolean = false, isIntrinsicConst: Boolean = false,
        fieldInit: IrExpression? = null,
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

            if (isIntrinsicConst) {
                property.annotations += intrinsicConstAnnotation
            }

            if (withGetter) {
                property.addGetter {
                    this.returnType = returnType
                    this.modality = modality
                    this.isOperator = false
                }.also { getter ->
                    getter.addDispatchReceiver { type = this@createProperty.defaultType }
                    getter.overriddenSymbols = property.overriddenSymbols.mapNotNull { it.owner.getter?.symbol }
                }
            }
            if (withField || fieldInit != null) {
                property.addBackingField {
                    this.type = returnType
                    this.isFinal = isConst
                }.also {
                    if (fieldInit != null) {
                        it.initializer = irFactory.createExpressionBody(0, 0) {
                            expression = fieldInit
                        }
                    }
                }
            }
            property.builder()
            components.symbolTable.declarePropertyWithSignature(
                irSignatureBuilder.computeSignature(property), property.symbol
            )
            property.getter?.let {
                components.symbolTable.declareSimpleFunctionWithSignature(
                    irSignatureBuilder.computeSignature(it), it.symbol
                )
            }
            property.backingField?.let {
                components.symbolTable.declareFieldWithSignature(
                    irSignatureBuilder.computeSignature(it), it.symbol
                )
            }
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

    private fun IrPackageFragment.createNumberClass(
        signature: IdSignature.CommonSignature,
        lazyContents: (IrClass.() -> Unit)? = null
    ) =
        createClass(this, signature) {
            configureSuperTypes(number)
            val thisType = defaultType
            createStandardNumericAndCharMembers(thisType)
            createStandardNumericMembers(thisType)
            if (thisType in primitiveIntegralIrTypes) {
                createStandardRangeMembers(thisType)
            }
            if (thisType == intType || thisType == longType) {
                createStandardBitwiseOps(thisType)
            }
            lazyContents?.invoke(this)
            createIntrinsicConstOfToStringAndEquals()
            finalizeClassDefinition()
        }

    private fun createPrimitiveArrayClass(
        parent: IrDeclarationParent,
        primitiveType: PrimitiveType,
        primitiveIterator: BuiltInClassValue
    ) =
        createClass(
            parent,
            getPublicSignature(parent.kotlinFqName, primitiveType.arrayTypeName.asString()),
            build = { modality = Modality.FINAL }
        ) {
            configureSuperTypes()
            primitiveIterator.ensureLazyContentsCreated()
            addArrayMembers(primitiveTypeToIrType[primitiveType]!!, primitiveIterator.type)
            finalizeClassDefinition()
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

    private fun IrClass.createStandardBitwiseOps(thisType: IrType) {
        for (op in bitwiseOperators) {
            createMemberFunction(op, thisType, "other" to thisType, isInfix = true)
        }
        for (op in shiftOperators) {
            createMemberFunction(op, thisType, "bitCount" to intType, isInfix = true)
        }
        createMemberFunction(OperatorNameConventions.INV, thisType)
    }

    private fun IrClass.createStandardRangeMembers(thisType: IrType) {
        for (argType in primitiveIntegralIrTypes) {
            createMemberFunction(
                OperatorNameConventions.RANGE_TO,
                if (thisType == longType || argType == longType) longRangeType else intRangeType,
                "other" to argType, isOperator = true, isIntrinsicConst = false
            )
        }
    }

    private fun IrClass.createStandardNumericMembers(thisType: IrType) {
        for (argument in primitiveNumericIrTypes) {
            createMemberFunction(
                OperatorNameConventions.COMPARE_TO, intType, "other" to argument,
                modality = if (argument == thisType) Modality.OPEN else Modality.FINAL,
                isOperator = true
            )
            val targetArithmeticReturnType = getPrimitiveArithmeticOperatorResultType(thisType, argument)
            for (op in arithmeticOperators) {
                createMemberFunction(op, targetArithmeticReturnType, "other" to argument, isOperator = true)
            }
        }
        val arithmeticReturnType = getPrimitiveArithmeticOperatorResultType(thisType, thisType)
        createMemberFunction(OperatorNameConventions.UNARY_PLUS, arithmeticReturnType, isOperator = true)
        createMemberFunction(OperatorNameConventions.UNARY_MINUS, arithmeticReturnType, isOperator = true)
    }

    private fun IrClass.createStandardNumericAndCharMembers(thisType: IrType) {
        createCompanionObject {
            val constExprs = getNumericConstantsExpressions(thisType)
            createProperty("MIN_VALUE", thisType, isConst = true, withGetter = false, fieldInit = constExprs.min)
            createProperty("MAX_VALUE", thisType, isConst = true, withGetter = false, fieldInit = constExprs.max)
            createProperty("SIZE_BYTES", intType, isConst = true, withGetter = false, fieldInit = constExprs.sizeBytes)
            createProperty("SIZE_BITS", intType, isConst = true, withGetter = false, fieldInit = constExprs.sizeBits)
        }
        for (targetPrimitive in primitiveIrTypesWithComparisons) {
            createMemberFunction("to${targetPrimitive.classFqName!!.shortName().asString()}", targetPrimitive, modality = Modality.OPEN)
        }
        createMemberFunction(OperatorNameConventions.INC, thisType, isOperator = true, isIntrinsicConst = false)
        createMemberFunction(OperatorNameConventions.DEC, thisType, isOperator = true, isIntrinsicConst = false)
    }

    private fun IrClass.createIntrinsicConstOfToStringAndEquals() {
        createMemberFunction(OperatorNameConventions.TO_STRING, stringType)
        createMemberFunction(
            OperatorNameConventions.EQUALS, booleanType, "other" to anyNType,
            modality = Modality.OPEN, isOperator = true
        )
    }

    private fun findFunctions(packageName: FqName, name: Name): List<IrSimpleFunctionSymbol> =
        components.session.symbolProvider.getTopLevelFunctionSymbols(packageName, name).mapNotNull { firOpSymbol ->
            components.declarationStorage.getIrFunctionSymbol(firOpSymbol) as? IrSimpleFunctionSymbol
        }
}
