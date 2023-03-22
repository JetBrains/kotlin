/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.descriptors

import org.jetbrains.kotlin.builtins.BuiltInsPackageFragment
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.BuiltInOperatorNames
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeBuilder
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.util.OperatorNameConventions

@ObsoleteDescriptorBasedAPI
class IrBuiltInsOverDescriptors(
    val builtIns: KotlinBuiltIns,
    private val typeTranslator: TypeTranslator,
    val symbolTable: SymbolTable
) : IrBuiltIns() {
    override val languageVersionSettings = typeTranslator.languageVersionSettings

    private var _functionFactory: IrAbstractDescriptorBasedFunctionFactory? = null
    var functionFactory: IrAbstractDescriptorBasedFunctionFactory
        get() =
            synchronized(this) {
                if (_functionFactory == null) {
                    _functionFactory = IrDescriptorBasedFunctionFactory(this, symbolTable, typeTranslator)
                }
                _functionFactory!!
            }
        set(value) {
            synchronized(this) {
                if (_functionFactory != null) {
                    error("functionFactory already set")
                } else {
                    _functionFactory = value
                }
            }
        }

    override val irFactory: IrFactory = symbolTable.irFactory

    private val builtInsModule = builtIns.builtInsModule

    private val kotlinInternalPackage = StandardClassIds.BASE_INTERNAL_PACKAGE
    private val kotlinInternalIrPackage = IrExternalPackageFragmentImpl.createEmptyExternalPackageFragment(builtInsModule, kotlinInternalPackage)

    private val packageFragmentDescriptor = IrBuiltinsPackageFragmentDescriptorImpl(builtInsModule, KOTLIN_INTERNAL_IR_FQN)
    override val operatorsPackageFragment: IrExternalPackageFragment =
        IrExternalPackageFragmentImpl(symbolTable.referenceExternalPackageFragment(packageFragmentDescriptor), KOTLIN_INTERNAL_IR_FQN)

    private fun ClassDescriptor.toIrSymbol() = symbolTable.referenceClass(this)
    private fun KotlinType.toIrType() = typeTranslator.translateType(this)

    private fun defineOperator(
        name: String, returnType: IrType, valueParameterTypes: List<IrType>, isIntrinsicConst: Boolean = false
    ): IrSimpleFunctionSymbol {
        val operatorDescriptor =
            IrSimpleBuiltinOperatorDescriptorImpl(packageFragmentDescriptor, Name.identifier(name), returnType.originalKotlinType!!)

        for ((i, valueParameterType) in valueParameterTypes.withIndex()) {
            operatorDescriptor.addValueParameter(
                IrBuiltinValueParameterDescriptorImpl(
                    operatorDescriptor, Name.identifier("arg$i"), i, valueParameterType.originalKotlinType!!
                )
            )
        }

        val symbol = symbolTable.declareSimpleFunctionIfNotExists(operatorDescriptor) {
            val operator = irFactory.createFunction(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                BUILTIN_OPERATOR,
                it,
                Name.identifier(name),
                DescriptorVisibilities.PUBLIC,
                Modality.FINAL,
                returnType,
                isInline = false,
                isExternal = false,
                isTailrec = false,
                isSuspend = false,
                isOperator = false,
                isInfix = false,
                isExpect = false,
                isFakeOverride = false
            )
            operator.parent = operatorsPackageFragment
            operatorsPackageFragment.declarations += operator

            operator.valueParameters = valueParameterTypes.withIndex().map { (i, valueParameterType) ->
                val valueParameterDescriptor = operatorDescriptor.valueParameters[i]
                val valueParameterSymbol = IrValueParameterSymbolImpl(valueParameterDescriptor)
                irFactory.createValueParameter(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, BUILTIN_OPERATOR, valueParameterSymbol, Name.identifier("arg$i"), i,
                    valueParameterType, null, isCrossinline = false, isNoinline = false, isHidden = false, isAssignable = false
                ).apply {
                    parent = operator
                }
            }

            if (isIntrinsicConst) {
                operator.annotations += IrConstructorCallImpl.fromSymbolDescriptor(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, intrinsicConstType, intrinsicConstConstructor.symbol
                )
            }

            operator
        }

        return symbol.symbol
    }

    private fun defineCheckNotNullOperator(): IrSimpleFunctionSymbol {
        val name = Name.identifier("CHECK_NOT_NULL")
        val typeParameterDescriptor: TypeParameterDescriptor
        val valueParameterDescriptor: ValueParameterDescriptor

        val returnKotlinType: SimpleType
        val valueKotlinType: SimpleType

        // Note: We still need a complete function descriptor here because `CHECK_NOT_NULL` is being substituted by psi2ir
        val operatorDescriptor = SimpleFunctionDescriptorImpl.create(
            packageFragmentDescriptor,
            Annotations.EMPTY,
            name,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE
        ).apply {
            typeParameterDescriptor = TypeParameterDescriptorImpl.createForFurtherModification(
                this, Annotations.EMPTY, false, Variance.INVARIANT, Name.identifier("T0"),
                0, SourceElement.NO_SOURCE, LockBasedStorageManager.NO_LOCKS
            ).apply {
                addUpperBound(any)
                setInitialized()
            }

            valueKotlinType = typeParameterDescriptor.typeConstructor.makeNullableType()

            valueParameterDescriptor = ValueParameterDescriptorImpl(
                this, null, 0, Annotations.EMPTY, Name.identifier("arg0"), valueKotlinType,
                declaresDefaultValue = false, isCrossinline = false, isNoinline = false, varargElementType = null,
                source = SourceElement.NO_SOURCE
            )

            returnKotlinType = typeParameterDescriptor.typeConstructor.makeNonNullType()

            initialize(
                null, null, listOf(), listOf(typeParameterDescriptor), listOf(valueParameterDescriptor), returnKotlinType,
                Modality.FINAL, DescriptorVisibilities.PUBLIC
            )
        }

        return symbolTable.declareSimpleFunctionIfNotExists(operatorDescriptor) { operatorSymbol ->
            val typeParameter = symbolTable.declareGlobalTypeParameter(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                BUILTIN_OPERATOR,
                typeParameterDescriptor
            ).apply {
                superTypes += anyType
            }
            val typeParameterSymbol = typeParameter.symbol

            val returnIrType = IrSimpleTypeBuilder().run {
                classifier = typeParameterSymbol
                kotlinType = returnKotlinType
                nullability = SimpleTypeNullability.DEFINITELY_NOT_NULL
                buildSimpleType()
            }

            val valueIrType = IrSimpleTypeBuilder().run {
                classifier = typeParameterSymbol
                kotlinType = valueKotlinType
                nullability = SimpleTypeNullability.MARKED_NULLABLE
                buildSimpleType()
            }

            irFactory.createFunction(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, BUILTIN_OPERATOR,
                operatorSymbol, name,
                DescriptorVisibilities.PUBLIC, Modality.FINAL,
                returnIrType,
                isInline = false, isExternal = false, isTailrec = false, isSuspend = false, isOperator = false, isInfix = false,
                isExpect = false, isFakeOverride = false
            ).also { operator ->
                operator.parent = operatorsPackageFragment
                operatorsPackageFragment.declarations += operator

                val valueParameterSymbol = IrValueParameterSymbolImpl(valueParameterDescriptor)
                val valueParameter = irFactory.createValueParameter(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, BUILTIN_OPERATOR, valueParameterSymbol, Name.identifier("arg0"), 0,
                    valueIrType, null, isCrossinline = false, isNoinline = false, isHidden = false, isAssignable = false
                )

                valueParameter.parent = operator
                typeParameter.parent = operator

                operator.valueParameters += valueParameter
                operator.typeParameters += typeParameter
            }
        }.symbol
    }

    private fun defineComparisonOperator(name: String, operandType: IrType) =
        defineOperator(name, booleanType, listOf(operandType, operandType), isIntrinsicConst = true)

    private fun List<IrType>.defineComparisonOperatorForEachIrType(name: String) =
        associate { it.classifierOrFail to defineComparisonOperator(name, it) }

    val any = builtIns.anyType
    override val anyType = any.toIrType()
    override val anyClass = builtIns.any.toIrSymbol()
    override val anyNType = anyType.makeNullable()

    private val intrinsicConstAnnotationFqName = kotlinInternalPackage.child(Name.identifier("IntrinsicConstEvaluation"))
    private val intrinsicConstClass = irFactory.buildClass {
        name = intrinsicConstAnnotationFqName.shortName()
        kind = ClassKind.ANNOTATION_CLASS
        modality = Modality.FINAL
    }.apply {
        parent = kotlinInternalIrPackage
        createImplicitParameterDeclarationWithWrappedDescriptor()
        addConstructor() { isPrimary = true }
        addFakeOverrides(IrTypeSystemContextImpl(this@IrBuiltInsOverDescriptors))
    }
    private val intrinsicConstType = intrinsicConstClass.defaultType
    private val intrinsicConstConstructor = intrinsicConstClass.primaryConstructor as IrConstructor

    val bool = builtIns.booleanType
    override val booleanType = bool.toIrType()
    override val booleanClass = builtIns.boolean.toIrSymbol()

    val char = builtIns.charType
    override val charType = char.toIrType()
    override val charClass = builtIns.char.toIrSymbol()

    val number = builtIns.number.defaultType
    override val numberType = number.toIrType()
    override val numberClass = builtIns.number.toIrSymbol()

    val byte = builtIns.byteType
    override val byteType = byte.toIrType()
    override val byteClass = builtIns.byte.toIrSymbol()

    val short = builtIns.shortType
    override val shortType = short.toIrType()
    override val shortClass = builtIns.short.toIrSymbol()

    val int = builtIns.intType
    override val intType = int.toIrType()
    override val intClass = builtIns.int.toIrSymbol()

    val long = builtIns.longType
    override val longType = long.toIrType()
    override val longClass = builtIns.long.toIrSymbol()

    val float = builtIns.floatType
    override val floatType = float.toIrType()
    override val floatClass = builtIns.float.toIrSymbol()

    val double = builtIns.doubleType
    override val doubleType = double.toIrType()
    override val doubleClass = builtIns.double.toIrSymbol()

    val nothing = builtIns.nothingType
    override val nothingType = nothing.toIrType()
    override val nothingClass = builtIns.nothing.toIrSymbol()
    override val nothingNType = nothingType.makeNullable()

    val unit = builtIns.unitType
    override val unitType = unit.toIrType()
    override val unitClass = builtIns.unit.toIrSymbol()

    val string = builtIns.stringType
    override val stringType = string.toIrType()
    override val stringClass = builtIns.string.toIrSymbol()

    // TODO: check if correct
    override val charSequenceClass = findClass(Name.identifier("CharSequence"), "kotlin")!!

    override val collectionClass = builtIns.collection.toIrSymbol()
    override val setClass = builtIns.set.toIrSymbol()
    override val listClass = builtIns.list.toIrSymbol()
    override val mapClass = builtIns.map.toIrSymbol()
    override val mapEntryClass = builtIns.mapEntry.toIrSymbol()
    override val iterableClass = builtIns.iterable.toIrSymbol()
    override val iteratorClass = builtIns.iterator.toIrSymbol()
    override val listIteratorClass = builtIns.listIterator.toIrSymbol()
    override val mutableCollectionClass = builtIns.mutableCollection.toIrSymbol()
    override val mutableSetClass = builtIns.mutableSet.toIrSymbol()
    override val mutableListClass = builtIns.mutableList.toIrSymbol()
    override val mutableMapClass = builtIns.mutableMap.toIrSymbol()
    override val mutableMapEntryClass = builtIns.mutableMapEntry.toIrSymbol()
    override val mutableIterableClass = builtIns.mutableIterable.toIrSymbol()
    override val mutableIteratorClass = builtIns.mutableIterator.toIrSymbol()
    override val mutableListIteratorClass = builtIns.mutableListIterator.toIrSymbol()
    override val comparableClass = builtIns.comparable.toIrSymbol()

    override val arrayClass = builtIns.array.toIrSymbol()

    override val throwableType = builtIns.throwable.defaultType.toIrType()
    override val throwableClass = builtIns.throwable.toIrSymbol()

    override val kCallableClass = builtIns.kCallable.toIrSymbol()
    override val kPropertyClass = builtIns.kProperty.toIrSymbol()
    override val kClassClass = builtIns.kClass.toIrSymbol()

    override val kProperty0Class = builtIns.kProperty0.toIrSymbol()
    override val kProperty1Class = builtIns.kProperty1.toIrSymbol()
    override val kProperty2Class = builtIns.kProperty2.toIrSymbol()
    override val kMutableProperty0Class = builtIns.kMutableProperty0.toIrSymbol()
    override val kMutableProperty1Class = builtIns.kMutableProperty1.toIrSymbol()
    override val kMutableProperty2Class = builtIns.kMutableProperty2.toIrSymbol()

    override val functionClass = builtIns.getBuiltInClassByFqName(FqName("kotlin.Function")).toIrSymbol()
    override val kFunctionClass = builtIns.getBuiltInClassByFqName(FqName("kotlin.reflect.KFunction")).toIrSymbol()

    override val annotationClass: IrClassSymbol = builtIns.annotation.toIrSymbol()
    override val annotationType: IrType = builtIns.annotationType.toIrType()

    override fun getKPropertyClass(mutable: Boolean, n: Int): IrClassSymbol = when (n) {
        0 -> if (mutable) kMutableProperty0Class else kProperty0Class
        1 -> if (mutable) kMutableProperty1Class else kProperty1Class
        2 -> if (mutable) kMutableProperty2Class else kProperty2Class
        else -> error("No KProperty for n=$n mutable=$mutable")
    }

    override val primitiveTypeToIrType = mapOf(
        PrimitiveType.BOOLEAN to booleanType,
        PrimitiveType.CHAR to charType,
        PrimitiveType.BYTE to byteType,
        PrimitiveType.SHORT to shortType,
        PrimitiveType.INT to intType,
        PrimitiveType.FLOAT to floatType,
        PrimitiveType.LONG to longType,
        PrimitiveType.DOUBLE to doubleType
    )

    // TODO switch to IrType
    val primitiveTypes = listOf(bool, char, byte, short, int, float, long, double)
    override val primitiveIrTypes = listOf(booleanType, charType, byteType, shortType, intType, floatType, longType, doubleType)
    override val primitiveIrTypesWithComparisons = listOf(charType, byteType, shortType, intType, floatType, longType, doubleType)
    override val primitiveFloatingPointIrTypes = listOf(floatType, doubleType)

    override val byteArray = builtIns.getPrimitiveArrayClassDescriptor(PrimitiveType.BYTE).toIrSymbol()
    override val charArray = builtIns.getPrimitiveArrayClassDescriptor(PrimitiveType.CHAR).toIrSymbol()
    override val shortArray = builtIns.getPrimitiveArrayClassDescriptor(PrimitiveType.SHORT).toIrSymbol()
    override val intArray = builtIns.getPrimitiveArrayClassDescriptor(PrimitiveType.INT).toIrSymbol()
    override val longArray = builtIns.getPrimitiveArrayClassDescriptor(PrimitiveType.LONG).toIrSymbol()
    override val floatArray = builtIns.getPrimitiveArrayClassDescriptor(PrimitiveType.FLOAT).toIrSymbol()
    override val doubleArray = builtIns.getPrimitiveArrayClassDescriptor(PrimitiveType.DOUBLE).toIrSymbol()
    override val booleanArray = builtIns.getPrimitiveArrayClassDescriptor(PrimitiveType.BOOLEAN).toIrSymbol()

    override val primitiveArraysToPrimitiveTypes =
        PrimitiveType.values().associate { builtIns.getPrimitiveArrayClassDescriptor(it).toIrSymbol() to it }
    override val primitiveTypesToPrimitiveArrays = primitiveArraysToPrimitiveTypes.map { (k, v) -> v to k }.toMap()
    override val primitiveArrayElementTypes = primitiveArraysToPrimitiveTypes.mapValues { primitiveTypeToIrType[it.value] }
    override val primitiveArrayForType = primitiveArrayElementTypes.asSequence().associate { it.value to it.key }

    override val unsignedTypesToUnsignedArrays: Map<UnsignedType, IrClassSymbol> =
        UnsignedType.values().mapNotNull { unsignedType ->
            val array = builtIns.builtInsModule.findClassAcrossModuleDependencies(unsignedType.arrayClassId)?.toIrSymbol()
            if (array == null) null else unsignedType to array
        }.toMap()

    override val unsignedArraysElementTypes: Map<IrClassSymbol, IrType?> by lazy {
        unsignedTypesToUnsignedArrays.map { (k, v) ->
            v to builtIns.builtInsModule.findClassAcrossModuleDependencies(k.classId)?.defaultType?.toIrType()
        }.toMap()
    }

    override val lessFunByOperandType = primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(BuiltInOperatorNames.LESS)
    override val lessOrEqualFunByOperandType =
        primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(BuiltInOperatorNames.LESS_OR_EQUAL)
    override val greaterOrEqualFunByOperandType =
        primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(BuiltInOperatorNames.GREATER_OR_EQUAL)
    override val greaterFunByOperandType =
        primitiveIrTypesWithComparisons.defineComparisonOperatorForEachIrType(BuiltInOperatorNames.GREATER)

    override val ieee754equalsFunByOperandType =
        primitiveFloatingPointIrTypes.map {
            it.classifierOrFail to defineOperator(
                BuiltInOperatorNames.IEEE754_EQUALS,
                booleanType,
                listOf(it.makeNullable(), it.makeNullable()),
                isIntrinsicConst = true
            )
        }.toMap()

    val booleanNot =
        builtIns.boolean.unsubstitutedMemberScope.getContributedFunctions(Name.identifier("not"), NoLookupLocation.FROM_BACKEND).single()
    override val booleanNotSymbol = symbolTable.referenceSimpleFunction(booleanNot)

    override val eqeqeqSymbol = defineOperator(BuiltInOperatorNames.EQEQEQ, booleanType, listOf(anyNType, anyNType))
    override val eqeqSymbol = defineOperator(BuiltInOperatorNames.EQEQ, booleanType, listOf(anyNType, anyNType), isIntrinsicConst = true)
    override val throwCceSymbol = defineOperator(BuiltInOperatorNames.THROW_CCE, nothingType, listOf())
    override val throwIseSymbol = defineOperator(BuiltInOperatorNames.THROW_ISE, nothingType, listOf())
    override val andandSymbol = defineOperator(BuiltInOperatorNames.ANDAND, booleanType, listOf(booleanType, booleanType), isIntrinsicConst = true)
    override val ororSymbol = defineOperator(BuiltInOperatorNames.OROR, booleanType, listOf(booleanType, booleanType), isIntrinsicConst = true)
    override val noWhenBranchMatchedExceptionSymbol =
        defineOperator(BuiltInOperatorNames.NO_WHEN_BRANCH_MATCHED_EXCEPTION, nothingType, listOf())
    override val illegalArgumentExceptionSymbol =
        defineOperator(BuiltInOperatorNames.ILLEGAL_ARGUMENT_EXCEPTION, nothingType, listOf(stringType))

    override val checkNotNullSymbol = defineCheckNotNullOperator()

    private fun TypeConstructor.makeNonNullType() = KotlinTypeFactory.simpleType(TypeAttributes.Empty, this, listOf(), false)
    private fun TypeConstructor.makeNullableType() = KotlinTypeFactory.simpleType(TypeAttributes.Empty, this, listOf(), true)

    override val dataClassArrayMemberHashCodeSymbol = defineOperator("dataClassArrayMemberHashCode", intType, listOf(anyType))

    override val dataClassArrayMemberToStringSymbol = defineOperator("dataClassArrayMemberToString", stringType, listOf(anyNType))

    override val intTimesSymbol: IrSimpleFunctionSymbol =
        builtIns.int.unsubstitutedMemberScope.findFirstFunction("times") {
            KotlinTypeChecker.DEFAULT.equalTypes(it.valueParameters[0].type, int)
        }.let { symbolTable.referenceSimpleFunction(it) }

    override val intXorSymbol: IrSimpleFunctionSymbol =
        builtIns.int.unsubstitutedMemberScope.findFirstFunction("xor") {
            KotlinTypeChecker.DEFAULT.equalTypes(it.valueParameters[0].type, int)
        }.let { symbolTable.referenceSimpleFunction(it) }

    override val intPlusSymbol: IrSimpleFunctionSymbol =
        builtIns.int.unsubstitutedMemberScope.findFirstFunction("plus") {
            KotlinTypeChecker.DEFAULT.equalTypes(it.valueParameters[0].type, int)
        }.let { symbolTable.referenceSimpleFunction(it) }

    override val arrayOf = findFunctions(Name.identifier("arrayOf")).first {
        it.descriptor.extensionReceiverParameter == null && it.descriptor.dispatchReceiverParameter == null &&
                it.descriptor.valueParameters.size == 1 && it.descriptor.valueParameters[0].varargElementType != null
    }

    override val arrayOfNulls = findFunctions(Name.identifier("arrayOfNulls")).first {
        it.descriptor.extensionReceiverParameter == null && it.descriptor.dispatchReceiverParameter == null &&
                it.descriptor.valueParameters.size == 1 && KotlinBuiltIns.isInt(it.descriptor.valueParameters[0].type)
    }

    override val linkageErrorSymbol: IrSimpleFunctionSymbol = defineOperator("linkageError", nothingType, listOf(stringType))

    override val enumClass = builtIns.enum.toIrSymbol()

    private fun builtInsPackage(vararg packageNameSegments: String) =
        builtIns.builtInsModule.getPackage(FqName.fromSegments(listOf(*packageNameSegments))).memberScope

    override fun findFunctions(name: Name, vararg packageNameSegments: String): Iterable<IrSimpleFunctionSymbol> =
        builtInsPackage(*packageNameSegments).getContributedFunctions(name, NoLookupLocation.FROM_BACKEND).map {
            symbolTable.referenceSimpleFunction(it)
        }

    override fun findFunctions(name: Name, packageFqName: FqName): Iterable<IrSimpleFunctionSymbol> =
        builtIns.builtInsModule.getPackage(packageFqName).memberScope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND).map {
            symbolTable.referenceSimpleFunction(it)
        }

    override fun findClass(name: Name, vararg packageNameSegments: String): IrClassSymbol? =
        (builtInsPackage(*packageNameSegments).getContributedClassifier(
            name,
            NoLookupLocation.FROM_BACKEND
        ) as? ClassDescriptor)?.let { symbolTable.referenceClass(it) }

    override fun findClass(name: Name, packageFqName: FqName): IrClassSymbol? =
        findClassDescriptor(name, packageFqName)?.let { symbolTable.referenceClass(it) }

    fun findClassDescriptor(name: Name, packageFqName: FqName): ClassDescriptor? =
        builtIns.builtInsModule.getPackage(packageFqName).memberScope.getContributedClassifier(
            name,
            NoLookupLocation.FROM_BACKEND
        ) as? ClassDescriptor

    override fun findBuiltInClassMemberFunctions(builtInClass: IrClassSymbol, name: Name): Iterable<IrSimpleFunctionSymbol> =
        builtInClass.descriptor.unsubstitutedMemberScope
            .getContributedFunctions(name, NoLookupLocation.FROM_BACKEND)
            .map { symbolTable.referenceSimpleFunction(it) }

    private val binaryOperatorCache = mutableMapOf<Triple<Name, IrType, IrType>, IrSimpleFunctionSymbol>()

    override fun getBinaryOperator(name: Name, lhsType: IrType, rhsType: IrType): IrSimpleFunctionSymbol {
        require(lhsType is IrSimpleType) { "Expected IrSimpleType in getBinaryOperator, got $lhsType" }
        val classifier = lhsType.classifier
        require(classifier is IrClassSymbol && classifier.isBound) {
            "Expected a bound IrClassSymbol for lhsType in getBinaryOperator, got $classifier"
        }
        val key = Triple(name, lhsType, rhsType)
        return binaryOperatorCache.getOrPut(key) {
            classifier.functions.single {
                val function = it.owner
                function.name == name && function.valueParameters.size == 1 && function.valueParameters[0].type == rhsType
            }
        }
    }

    private val unaryOperatorCache = mutableMapOf<Pair<Name, IrType>, IrSimpleFunctionSymbol>()

    override fun getUnaryOperator(name: Name, receiverType: IrType): IrSimpleFunctionSymbol {
        require(receiverType is IrSimpleType) { "Expected IrSimpleType in getBinaryOperator, got $receiverType" }
        val classifier = receiverType.classifier
        require(classifier is IrClassSymbol && classifier.isBound) {
            "Expected a bound IrClassSymbol for receiverType in getBinaryOperator, got $classifier"
        }
        val key = Pair(name, receiverType)
        return unaryOperatorCache.getOrPut(key) {
            classifier.functions.single {
                val function = it.owner
                function.name == name && function.valueParameters.isEmpty()
            }
        }
    }

    private fun <T : Any> getFunctionsByKey(
        name: Name,
        vararg packageNameSegments: String,
        makeKey: (SimpleFunctionDescriptor) -> T?
    ): Map<T, IrSimpleFunctionSymbol> {
        val result = mutableMapOf<T, IrSimpleFunctionSymbol>()
        for (d in builtInsPackage(*packageNameSegments).getContributedFunctions(name, NoLookupLocation.FROM_BACKEND)) {
            makeKey(d)?.let { key ->
                result[key] = symbolTable.referenceSimpleFunction(d)
            }
        }
        return result
    }

    override fun getNonBuiltInFunctionsByExtensionReceiver(
        name: Name, vararg packageNameSegments: String
    ): Map<IrClassifierSymbol, IrSimpleFunctionSymbol> =
        getFunctionsByKey(name, *packageNameSegments) {
            if (it.containingDeclaration !is BuiltInsPackageFragment && it.extensionReceiverParameter != null) {
                symbolTable.referenceClassifier(it.extensionReceiverParameter!!.type.constructor.declarationDescriptor!!)
            } else null
        }

    override fun getNonBuiltinFunctionsByReturnType(
        name: Name, vararg packageNameSegments: String
    ): Map<IrClassifierSymbol, IrSimpleFunctionSymbol> =
        getFunctionsByKey(Name.identifier("getProgressionLastElement"), *packageNameSegments) { d ->
            if (d.containingDeclaration !is BuiltInsPackageFragment) {
                d.returnType?.constructor?.declarationDescriptor?.let { symbolTable.referenceClassifier(it) }
            } else null
        }

    override val extensionToString: IrSimpleFunctionSymbol = findFunctions(OperatorNameConventions.TO_STRING, "kotlin").first {
        val descriptor = it.descriptor
        descriptor is SimpleFunctionDescriptor && descriptor.dispatchReceiverParameter == null &&
                descriptor.extensionReceiverParameter != null &&
                KotlinBuiltIns.isNullableAny(descriptor.extensionReceiverParameter!!.type) && descriptor.valueParameters.isEmpty()
    }

    override val memberToString: IrSimpleFunctionSymbol = findBuiltInClassMemberFunctions(
        anyClass,
        OperatorNameConventions.TO_STRING
    ).single {
        val descriptor = it.descriptor
        descriptor is SimpleFunctionDescriptor && descriptor.valueParameters.isEmpty()
    }

    override val extensionStringPlus: IrSimpleFunctionSymbol = findFunctions(OperatorNameConventions.PLUS, "kotlin").first {
        val descriptor = it.descriptor
        descriptor is SimpleFunctionDescriptor && descriptor.dispatchReceiverParameter == null &&
                descriptor.extensionReceiverParameter != null &&
                KotlinBuiltIns.isStringOrNullableString(descriptor.extensionReceiverParameter!!.type) &&
                descriptor.valueParameters.size == 1 &&
                KotlinBuiltIns.isNullableAny(descriptor.valueParameters.first().type)
    }

    override val memberStringPlus: IrSimpleFunctionSymbol = findBuiltInClassMemberFunctions(
        stringClass,
        OperatorNameConventions.PLUS
    ).single {
        val descriptor = it.descriptor
        descriptor is SimpleFunctionDescriptor &&
                descriptor.valueParameters.size == 1 &&
                KotlinBuiltIns.isNullableAny(descriptor.valueParameters.first().type)
    }

    override fun functionN(arity: Int): IrClass = functionFactory.functionN(arity)
    override fun kFunctionN(arity: Int): IrClass = functionFactory.kFunctionN(arity)
    override fun suspendFunctionN(arity: Int): IrClass = functionFactory.suspendFunctionN(arity)
    override fun kSuspendFunctionN(arity: Int): IrClass = functionFactory.kSuspendFunctionN(arity)
}

private inline fun MemberScope.findFirstFunction(name: String, predicate: (CallableMemberDescriptor) -> Boolean) =
    getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND).first(predicate)
