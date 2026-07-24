/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns.Companion.BUILTIN_OPERATOR
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.expressions.impl.IrAnnotationImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.defaultTypeWithoutArguments
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.toIdSignature
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.Variance

@OptIn(InternalSymbolFinderAPI::class)
class IrSyntheticProvider(
    packageFragmentDescriptor: PackageFragmentDescriptor,
    private val symbolTable: SymbolTable,
    private val signatureComputer: (IrDeclaration) -> IdSignature,
) {
    private val irFactory: IrFactory = symbolTable.irFactory
    val operatorsPackageFragment = IrExternalPackageFragmentImpl(
        IrExternalPackageFragmentSymbolImpl(descriptor = packageFragmentDescriptor), StandardClassIds.BASE_INTERNAL_IR_PACKAGE
    )

    private val anyClass = symbolTable.referenceClass(StandardClassIds.Any.toIdSignature())
    private val anyType = anyClass.defaultTypeWithoutArguments
    private val anyNType = anyType.makeNullable()

    private val booleanClass = symbolTable.referenceClass(StandardClassIds.Boolean.toIdSignature())
    private val booleanType = booleanClass.defaultTypeWithoutArguments
    private val charClass = symbolTable.referenceClass(StandardClassIds.Char.toIdSignature())
    private val charType = charClass.defaultTypeWithoutArguments
    private val byteClass = symbolTable.referenceClass(StandardClassIds.Byte.toIdSignature())
    private val byteType = byteClass.defaultTypeWithoutArguments
    private val shortClass = symbolTable.referenceClass(StandardClassIds.Short.toIdSignature())
    private val shortType = shortClass.defaultTypeWithoutArguments
    private val intClass = symbolTable.referenceClass(StandardClassIds.Int.toIdSignature())
    private val intType = intClass.defaultTypeWithoutArguments
    private val longClass = symbolTable.referenceClass(StandardClassIds.Long.toIdSignature())
    private val longType = longClass.defaultTypeWithoutArguments
    private val floatClass = symbolTable.referenceClass(StandardClassIds.Float.toIdSignature())
    private val floatType = floatClass.defaultTypeWithoutArguments
    private val doubleClass = symbolTable.referenceClass(StandardClassIds.Double.toIdSignature())
    private val doubleType = doubleClass.defaultTypeWithoutArguments
    private val stringClass = symbolTable.referenceClass(StandardClassIds.String.toIdSignature())
    private val stringType = stringClass.defaultTypeWithoutArguments
    private val nothingClass = symbolTable.referenceClass(StandardClassIds.Nothing.toIdSignature())
    private val nothingType = nothingClass.defaultTypeWithoutArguments

    private val intrinsicConstAnnotation = symbolTable.referenceClass(StandardClassIds.Annotations.IntrinsicConstEvaluation.toIdSignature())

    private val primitiveTypeToIrType: Map<PrimitiveType, IrType> = mapOf(
        PrimitiveType.BOOLEAN to booleanType,
        PrimitiveType.CHAR to charType,
        PrimitiveType.BYTE to byteType,
        PrimitiveType.SHORT to shortType,
        PrimitiveType.INT to intType,
        PrimitiveType.LONG to longType,
        PrimitiveType.FLOAT to floatType,
        PrimitiveType.DOUBLE to doubleType
    )

    val ieee754equalsFunByOperandType: Map<PrimitiveType, IrSimpleFunctionSymbol> =
        listOf(PrimitiveType.FLOAT, PrimitiveType.DOUBLE).associateWith { primitiveType ->
            val fpType = primitiveTypeToIrType.getValue(primitiveType)
            createFunction(
                name = ieee754equals.callableName,
                returnType = booleanType,
                parent = operatorsPackageFragment,
                valueParameterTypes = arrayOf("arg0" to fpType.makeNullable(), "arg1" to fpType.makeNullable()),
            )
        }

    val eqeqeqSymbol: IrSimpleFunctionSymbol = createFunction(
        name = eqeqeq.callableName,
        returnType = booleanType,
        parent = operatorsPackageFragment,
        valueParameterTypes = arrayOf("" to anyNType, "" to anyNType),
    )

    val eqeqSymbol: IrSimpleFunctionSymbol = createFunction(
        name = eqeq.callableName,
        returnType = booleanType,
        parent = operatorsPackageFragment,
        valueParameterTypes = arrayOf("" to anyNType, "" to anyNType),
    )

    val throwCceSymbol: IrSimpleFunctionSymbol = createFunction(
        name = throwCce.callableName,
        returnType = nothingType,
        parent = operatorsPackageFragment,
        valueParameterTypes = arrayOf<Pair<String, IrType>>(),
    )

    val throwIseSymbol: IrSimpleFunctionSymbol = createFunction(
        name = throwIse.callableName,
        returnType = nothingType,
        parent = operatorsPackageFragment,
        valueParameterTypes = arrayOf<Pair<String, IrType>>(),
    )

    val andandSymbol: IrSimpleFunctionSymbol = createFunction(
        name = andand.callableName,
        returnType = booleanType,
        parent = operatorsPackageFragment,
        valueParameterTypes = arrayOf("" to booleanType, "" to booleanType),
    )

    val ororSymbol: IrSimpleFunctionSymbol = createFunction(
        name = oror.callableName,
        returnType = booleanType,
        parent = operatorsPackageFragment,
        valueParameterTypes = arrayOf("" to booleanType, "" to booleanType),
    )

    val noWhenBranchMatchedExceptionSymbol: IrSimpleFunctionSymbol = createFunction(
        name = noWhenBranchMatchedException.callableName,
        returnType = nothingType,
        parent = operatorsPackageFragment,
        valueParameterTypes = arrayOf<Pair<String, IrType>>(),
    )

    val illegalArgumentExceptionSymbol: IrSimpleFunctionSymbol = createFunction(
        name = illegalArgumentException.callableName,
        returnType = nothingType,
        parent = operatorsPackageFragment,
        valueParameterTypes = arrayOf("" to stringType),
    )

    val dataClassArrayMemberHashCodeSymbol: IrSimpleFunctionSymbol = createFunction(
        name = dataClassArrayMemberHashCode.callableName,
        returnType = intType,
        parent = operatorsPackageFragment,
        valueParameterTypes = arrayOf("" to anyType),
    )

    val dataClassArrayMemberToStringSymbol: IrSimpleFunctionSymbol = createFunction(
        name = dataClassArrayMemberToString.callableName,
        returnType = stringType,
        parent = operatorsPackageFragment,
        valueParameterTypes = arrayOf("" to anyNType),
    )

    val checkNotNullSymbol: IrSimpleFunctionSymbol = run {
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
            name = checkNotNull.callableName,
            returnType = IrSimpleTypeImpl(typeParameter.symbol, SimpleTypeNullability.DEFINITELY_NOT_NULL, emptyList(), emptyList()),
            parent = operatorsPackageFragment,
            valueParameterTypes = arrayOf("" to IrSimpleTypeImpl(typeParameter.symbol, hasQuestionMark = true, emptyList(), emptyList())),
            typeParameters = listOf(typeParameter),
            origin = BUILTIN_OPERATOR,
        )
    }

    val linkageErrorSymbol: IrSimpleFunctionSymbol = createFunction(
        name = linkageError.callableName,
        returnType = nothingType,
        parent = operatorsPackageFragment,
        valueParameterTypes = arrayOf("" to anyNType),
    )

    val lessFunByOperandType: Map<PrimitiveType, IrSimpleFunctionSymbol> =
        defineComparisonOperatorForEachIrType(less.callableName)
    val lessOrEqualFunByOperandType: Map<PrimitiveType, IrSimpleFunctionSymbol> =
        defineComparisonOperatorForEachIrType(lessOrEqual.callableName)
    val greaterOrEqualFunByOperandType: Map<PrimitiveType, IrSimpleFunctionSymbol> =
        defineComparisonOperatorForEachIrType(greaterOrEqual.callableName)
    val greaterFunByOperandType: Map<PrimitiveType, IrSimpleFunctionSymbol> =
        defineComparisonOperatorForEachIrType(greater.callableName)

    private fun createFunction(
        name: Name,
        returnType: IrType,
        parent: IrExternalPackageFragment,
        valueParameterTypes: Array<out Pair<String, IrType>>,
        typeParameters: List<IrTypeParameter> = emptyList(),
        origin: IrDeclarationOrigin = BUILTIN_OPERATOR,
    ): IrSimpleFunctionSymbol {
        val function = irFactory.createFunctionWithLateBinding(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = origin,
            name = name,
            visibility = DescriptorVisibilities.PUBLIC,
            isInline = false,
            isExpect = false,
            returnType = returnType,
            modality = Modality.FINAL,
            isTailrec = false,
            isSuspend = false,
            isOperator = false,
            isInfix = false,
            isExternal = false,
            isFakeOverride = false,
        ).also { fn ->
            valueParameterTypes.forEachIndexed { index, [pName, irType] ->
                fn.addValueParameter(Name.identifier(pName.ifBlank { "arg$index" }), irType, origin)
            }
            fn.typeParameters = typeParameters
            typeParameters.forEach { it.parent = fn }
            fn.parent = parent
            parent.declarations.add(fn)
        }

        val signature = signatureComputer(function)
        val symbol = symbolTable.referenceSimpleFunction(signature)
        // TODO KT-84836 Drop this check. Symbol is bound because we use old `IrBuiltInsOverDescriptors`. It should be replaced with `IrBuiltInsOverLinker`
        if (symbol.isBound) {
            parent.declarations.remove(function)
            return symbol
        }
        function.acquireSymbol(symbol)
        return symbol
    }

    private fun defineComparisonOperatorForEachIrType(name: Name): Map<PrimitiveType, IrSimpleFunctionSymbol> {
        return PrimitiveType.NUMBER_TYPES.associateWith { primitiveType ->
            val irType = primitiveTypeToIrType.getValue(primitiveType)
            createFunction(
                name = name,
                returnType = booleanType,
                parent = operatorsPackageFragment,
                valueParameterTypes = arrayOf("" to irType, "" to irType),
            )
        }
    }

    private fun intrinsicConstAnnotationCall(): IrAnnotation {
        return IrAnnotationImpl(
            startOffset = UNDEFINED_OFFSET, endOffset = UNDEFINED_OFFSET,
            type = IrSimpleTypeImpl(
                classifier = intrinsicConstAnnotation,
                nullability = SimpleTypeNullability.DEFINITELY_NOT_NULL,
                arguments = emptyList(),
                annotations = emptyList()
            ),
            symbol = intrinsicConstAnnotation.owner.primaryConstructor!!.symbol,
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0,
            origin = null
        )
    }

    public fun finish() {
        fun IrSimpleFunctionSymbol.applyIntrinsicConstAnnotation(): IrSimpleFunctionSymbol {
            return apply {
                owner.annotations += intrinsicConstAnnotationCall()
            }
        }

        ieee754equalsFunByOperandType.values.forEach { it.applyIntrinsicConstAnnotation() }
        eqeqSymbol.applyIntrinsicConstAnnotation()
        andandSymbol.applyIntrinsicConstAnnotation()
        ororSymbol.applyIntrinsicConstAnnotation()
        lessFunByOperandType.values.forEach { it.applyIntrinsicConstAnnotation() }
        lessOrEqualFunByOperandType.values.forEach { it.applyIntrinsicConstAnnotation() }
        greaterOrEqualFunByOperandType.values.forEach { it.applyIntrinsicConstAnnotation() }
        greaterFunByOperandType.values.forEach { it.applyIntrinsicConstAnnotation() }
    }

    companion object {
        private const val dataClassArrayMemberHashCodeName = "dataClassArrayMemberHashCode"
        private const val dataClassArrayMemberToStringName = "dataClassArrayMemberToString"
        private const val linkageErrorSymbolName = "linkageErrorSymbol"

        private fun String.toCallableId(): CallableId = CallableId(StandardClassIds.BASE_INTERNAL_IR_PACKAGE, Name.identifier(this))
        val ieee754equals = BuiltInOperatorNames.IEEE754_EQUALS.toCallableId()
        val eqeqeq = BuiltInOperatorNames.EQEQEQ.toCallableId()
        val eqeq = BuiltInOperatorNames.EQEQ.toCallableId()
        val throwCce = BuiltInOperatorNames.THROW_CCE.toCallableId()
        val throwIse = BuiltInOperatorNames.THROW_ISE.toCallableId()
        val andand = BuiltInOperatorNames.ANDAND.toCallableId()
        val oror = BuiltInOperatorNames.OROR.toCallableId()
        val noWhenBranchMatchedException = BuiltInOperatorNames.NO_WHEN_BRANCH_MATCHED_EXCEPTION.toCallableId()
        val illegalArgumentException = BuiltInOperatorNames.ILLEGAL_ARGUMENT_EXCEPTION.toCallableId()
        val dataClassArrayMemberHashCode = dataClassArrayMemberHashCodeName.toCallableId()
        val dataClassArrayMemberToString = dataClassArrayMemberToStringName.toCallableId()
        val checkNotNull = BuiltInOperatorNames.CHECK_NOT_NULL.toCallableId()
        val linkageError = linkageErrorSymbolName.toCallableId()
        val less = BuiltInOperatorNames.LESS.toCallableId()
        val lessOrEqual = BuiltInOperatorNames.LESS_OR_EQUAL.toCallableId()
        val greaterOrEqual = BuiltInOperatorNames.GREATER_OR_EQUAL.toCallableId()
        val greater = BuiltInOperatorNames.GREATER.toCallableId()
    }
}
