/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.descriptors

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.expressions.impl.IrAnnotationImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeBuilder
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.util.OperatorNameConventions

@ObsoleteDescriptorBasedAPI
@OptIn(InternalSymbolFinderAPI::class)
class IrBuiltInsOverDescriptors(
    val builtIns: KotlinBuiltIns,
    private val typeTranslator: TypeTranslator,
    val symbolTable: SymbolTable
) : IrBuiltIns(SymbolFinderOverDescriptors(builtIns, symbolTable)) {
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
    override val kotlinInternalPackageFragment = createEmptyExternalPackageFragment(builtInsModule, kotlinInternalPackage)

    private val packageFragmentDescriptor = IrBuiltinsPackageFragmentDescriptorImpl(builtInsModule, KOTLIN_INTERNAL_IR_FQN)

    /*
     * In REPL it's possible that builtins will be created several times with the same symbol table
     * And since IrBuiltinsPackageFragmentDescriptorImpl has overridden equals, symbol for external package
     *   will be the same for different descriptors (with same FQN). So we should create IrExternalPackageFragment
     *   here only if it was not created before, on previous compilation
     */
    override val operatorsPackageFragment: IrExternalPackageFragment =
        symbolTable.descriptorExtension.declareExternalPackageFragmentIfNotExists(packageFragmentDescriptor)

    private fun ClassDescriptor.toIrSymbol(): IrClassSymbol {
        return symbolTable.descriptorExtension.referenceClass(this)
    }

    private fun FunctionDescriptor.toIrSymbol(): IrSimpleFunctionSymbol {
        return symbolTable.descriptorExtension.referenceSimpleFunction(this)
    }

    private fun PropertyDescriptor.toIrSymbol(): IrPropertySymbol {
        return symbolTable.descriptorExtension.referenceProperty(this)
    }

    private fun defineOperator(
        name: String, returnType: IrType, valueParameterTypes: List<IrType>, isIntrinsicConst: Boolean = false
    ): IrSimpleFunctionSymbol {
        val operatorDescriptor =
            IrSimpleBuiltinOperatorDescriptorImpl(packageFragmentDescriptor, Name.identifier(name), returnType.toIrBasedKotlinType())

        for ((i, valueParameterType) in valueParameterTypes.withIndex()) {
            operatorDescriptor.addValueParameter(
                IrBuiltinValueParameterDescriptorImpl(
                    operatorDescriptor, Name.identifier("arg$i"), i, valueParameterType.toIrBasedKotlinType()
                )
            )
        }

        val symbol = symbolTable.descriptorExtension.declareSimpleFunctionIfNotExists(operatorDescriptor) {
            val operator = irFactory.createSimpleFunction(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                origin = BUILTIN_OPERATOR,
                name = Name.identifier(name),
                visibility = DescriptorVisibilities.PUBLIC,
                isInline = false,
                isExpect = false,
                returnType = returnType,
                modality = Modality.FINAL,
                symbol = it,
                isTailrec = false,
                isSuspend = false,
                isOperator = false,
                isInfix = false,
                isExternal = false,
            )
            operator.parent = operatorsPackageFragment
            operatorsPackageFragment.declarations += operator

            operator.parameters += valueParameterTypes.withIndex().map { (i, valueParameterType) ->
                val valueParameterDescriptor = operatorDescriptor.valueParameters[i]
                val valueParameterSymbol = IrValueParameterSymbolImpl(valueParameterDescriptor)
                irFactory.createValueParameter(
                    startOffset = UNDEFINED_OFFSET,
                    endOffset = UNDEFINED_OFFSET,
                    origin = BUILTIN_OPERATOR,
                    kind = IrParameterKind.Regular,
                    name = Name.identifier("arg$i"),
                    type = valueParameterType,
                    isAssignable = false,
                    symbol = valueParameterSymbol,
                    varargElementType = null,
                    isCrossinline = false,
                    isNoinline = false,
                    isHidden = false
                ).apply {
                    parent = operator
                }
            }

            if (isIntrinsicConst) {
                operator.annotations += IrAnnotationImpl.fromSymbolDescriptor(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, intrinsicConstType, intrinsicConstConstructor.symbol
                )
            }

            operator
        }

        return symbol.symbol
    }

    private fun defineCheckNotNullOperator(): IrSimpleFunctionSymbol {
        val name = Name.identifier(BuiltInOperatorNames.CHECK_NOT_NULL)
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

        return symbolTable.descriptorExtension.declareSimpleFunctionIfNotExists(operatorDescriptor) { operatorSymbol ->
            val typeParameter = symbolTable.descriptorExtension.declareGlobalTypeParameter(
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

            irFactory.createSimpleFunction(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                origin = BUILTIN_OPERATOR,
                name = name,
                visibility = DescriptorVisibilities.PUBLIC,
                isInline = false,
                isExpect = false,
                returnType = returnIrType,
                modality = Modality.FINAL,
                symbol = operatorSymbol,
                isTailrec = false,
                isSuspend = false,
                isOperator = false,
                isInfix = false,
                isExternal = false,
            ).also { operator ->
                operator.parent = operatorsPackageFragment
                operatorsPackageFragment.declarations += operator

                val valueParameterSymbol = IrValueParameterSymbolImpl(valueParameterDescriptor)
                val valueParameter = irFactory.createValueParameter(
                    startOffset = UNDEFINED_OFFSET,
                    endOffset = UNDEFINED_OFFSET,
                    origin = BUILTIN_OPERATOR,
                    kind = IrParameterKind.Regular,
                    name = Name.identifier("arg0"),
                    type = valueIrType,
                    isAssignable = false,
                    symbol = valueParameterSymbol,
                    varargElementType = null,
                    isCrossinline = false,
                    isNoinline = false,
                    isHidden = false,
                )

                valueParameter.parent = operator
                typeParameter.parent = operator

                operator.parameters += valueParameter
                operator.typeParameters += typeParameter
            }
        }.symbol
    }

    private fun defineComparisonOperator(name: String, operandType: IrType) =
        defineOperator(name, booleanType, listOf(operandType, operandType), isIntrinsicConst = true)

    private fun List<IrType>.defineComparisonOperatorForEachIrType(name: String) =
        associate { it.classifierOrFail to defineComparisonOperator(name, it) }

    val any = builtIns.anyType
    val char = builtIns.charType
    val number = builtIns.number.defaultType
    val byte = builtIns.byteType
    val short = builtIns.shortType
    val int = builtIns.intType
    val long = builtIns.longType
    val float = builtIns.floatType
    val double = builtIns.doubleType
    val nothing = builtIns.nothingType
    val unit = builtIns.unitType
    val string = builtIns.stringType

    private val intrinsicConstClass = createIntrinsicConstEvaluationClass()
    private val intrinsicConstType = intrinsicConstClass.defaultType
    private val intrinsicConstConstructor = intrinsicConstClass.primaryConstructor as IrConstructor


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
    override val booleanNotSymbol = booleanNot.toIrSymbol()

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

    override val linkageErrorSymbol: IrSimpleFunctionSymbol = defineOperator("linkageError", nothingType, listOf(stringType))

    override fun functionN(arity: Int): IrClass = functionFactory.functionN(arity)
    override fun kFunctionN(arity: Int): IrClass = functionFactory.kFunctionN(arity)
    override fun suspendFunctionN(arity: Int): IrClass = functionFactory.suspendFunctionN(arity)
    override fun kSuspendFunctionN(arity: Int): IrClass = functionFactory.kSuspendFunctionN(arity)
}

@InternalSymbolFinderAPI
class SymbolFinderOverDescriptors(private val builtIns: KotlinBuiltIns, private val symbolTable: SymbolTable) : SymbolFinder() {
    private fun getClassDescriptor(classId: ClassId) : ClassDescriptor? {
        val parentClassId = classId.parentClassId
        return if (parentClassId == null) {
            builtIns.builtInsModule
                .getPackage(classId.packageFqName)
                .memberScope
        } else {
            getClassDescriptor(parentClassId)?.unsubstitutedInnerClassesScope
        }?.getContributedClassifier(classId.shortClassName, NoLookupLocation.FROM_BACKEND) as? ClassDescriptor
    }

    override fun findClass(classId: ClassId): IrClassSymbol? {
        return getClassDescriptor(classId)?.toIrSymbol()
    }

    private fun getScopeToLookup(callableId: CallableId) : MemberScope? {
        val classId = callableId.classId
        return if (classId == null) {
            builtIns.builtInsModule.getPackage(callableId.packageName).memberScope
        } else {
            getClassDescriptor(classId)?.unsubstitutedMemberScope
        }
    }

    override fun findFunctions(callableId: CallableId): Iterable<IrSimpleFunctionSymbol> {
        return getScopeToLookup(callableId)
            ?.getContributedFunctions(callableId.callableName, NoLookupLocation.FROM_BACKEND)
            .orEmpty()
            .map { it.toIrSymbol() }
    }

    override fun findProperties(callableId: CallableId): Iterable<IrPropertySymbol> {
        return getScopeToLookup(callableId)
            ?.getContributedVariables(callableId.callableName, NoLookupLocation.FROM_BACKEND)
            .orEmpty()
            .map { it.toIrSymbol() }
    }

    private fun ClassDescriptor.toIrSymbol(): IrClassSymbol {
        return symbolTable.descriptorExtension.referenceClass(this)
    }

    private fun FunctionDescriptor.toIrSymbol(): IrSimpleFunctionSymbol {
        return symbolTable.descriptorExtension.referenceSimpleFunction(this)
    }

    private fun PropertyDescriptor.toIrSymbol(): IrPropertySymbol {
        return symbolTable.descriptorExtension.referenceProperty(this)
    }
}
