/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.builders

import org.jetbrains.kotlin.bir.SourceSpan
import org.jetbrains.kotlin.bir.backend.BirBackendContext
import org.jetbrains.kotlin.bir.backend.utils.listOfNulls
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.declarations.impl.BirVariableImpl
import org.jetbrains.kotlin.bir.expressions.*
import org.jetbrains.kotlin.bir.expressions.impl.*
import org.jetbrains.kotlin.bir.resetWithNulls
import org.jetbrains.kotlin.bir.symbols.*
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.types.utils.defaultType
import org.jetbrains.kotlin.bir.types.utils.typeWith
import org.jetbrains.kotlin.bir.util.constructedClass
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.name.Name
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


context(BirBackendContext)
@OptIn(ExperimentalContracts::class)
inline fun <R> birBodyScope(block: context(BirStatementBuilderScope) () -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return block(BirStatementBuilderScope())
}

context(BirBackendContext)
class BirStatementBuilderScope() {
    var sourceSpan: SourceSpan = SourceSpan.UNDEFINED
    var origin: IrStatementOrigin? = null
    var returnTarget: BirReturnTargetSymbol? = null

    private var lastTemporaryIndex: Int = 0
    private fun nextTemporaryIndex(): Int = lastTemporaryIndex++

    fun birNull(type: BirType = birBuiltIns.nothingNType) = BirConst.constNull(sourceSpan, type)
    fun birConst(value: Boolean) = BirConst(value, sourceSpan)
    fun birConst(value: Byte) = BirConst(value, sourceSpan)
    fun birConst(value: Short) = BirConst(value, sourceSpan)
    fun birConst(value: Int) = BirConst(value, sourceSpan)
    fun birConst(value: Long) = BirConst(value, sourceSpan)
    fun birConst(value: Float) = BirConst(value, sourceSpan)
    fun birConst(value: Double) = BirConst(value, sourceSpan)
    fun birConst(value: Char) = BirConst(value, sourceSpan)
    fun birConst(value: String) = BirConst(value, sourceSpan)

    fun birConstantPrimitive(
        value: BirConst<*>,
        block: BirConstantPrimitive.() -> Unit = {},
    ): BirConstantPrimitive =
        BirConstantPrimitiveImpl(sourceSpan, value.type, value).apply(block)

    fun birGetUnit(): BirGetObjectValue = BirGetObjectValueImpl(sourceSpan, birBuiltIns.unitType, birBuiltIns.unitClass)


    fun birGet(
        variable: BirValueDeclaration,
        type: BirType = variable.type,
        origin: IrStatementOrigin? = this.origin,
        block: BirGetValue.() -> Unit = {},
    ): BirGetValue =
        BirGetValueImpl(sourceSpan, variable.type, variable, origin).apply(block)

    fun birSet(
        variable: BirValueDeclaration,
        value: BirExpression,
        origin: IrStatementOrigin? = this.origin,
        block: BirSetValue.() -> Unit = {},
    ): BirSetValue =
        BirSetValueImpl(sourceSpan, birBuiltIns.unitType, variable, origin, value).apply(block)


    fun birGetField(
        receiver: BirExpression?,
        field: BirField,
        type: BirType = field.type,
        origin: IrStatementOrigin? = this@BirStatementBuilderScope.origin,
        block: BirGetField.() -> Unit = {},
    ): BirGetField =
        BirGetFieldImpl(sourceSpan, type, field, null, receiver, origin).apply(block)

    fun birSetField(
        receiver: BirExpression?,
        field: BirField,
        value: BirExpression,
        origin: IrStatementOrigin? = this@BirStatementBuilderScope.origin,
        block: BirSetField.() -> Unit = {},
    ): BirSetField =
        BirSetFieldImpl(sourceSpan, birBuiltIns.unitType, field, null, receiver, origin, value).apply(block).apply(block)


    fun birGetObjectValue(
        clazz: BirClassSymbol,
        type: BirType = clazz.defaultType,
    ): BirGetObjectValue =
        BirGetObjectValueImpl(sourceSpan, type, clazz)


    fun birWhen(
        type: BirType,
        origin: IrStatementOrigin? = this@BirStatementBuilderScope.origin,
        block: BirWhen.() -> Unit = {},
    ): BirWhen =
        BirWhenImpl(sourceSpan, type, origin).apply(block)

    fun birBranch(
        condition: BirExpression,
        result: BirExpression,
        block: BirBranch.() -> Unit = {},
    ): BirBranch =
        BirBranchImpl(sourceSpan, condition, result).apply(block)

    fun birElseBranch(
        result: BirExpression,
        block: BirElseBranch.() -> Unit = {},
    ): BirElseBranch =
        BirElseBranchImpl(sourceSpan, birConst(true), result).apply(block)

    fun birReturn(
        value: BirExpression,
        returnTarget: BirReturnTargetSymbol = this.returnTarget ?: error("return target not specified"),
        block: BirReturn.() -> Unit = {},
    ): BirReturn =
        BirReturnImpl(sourceSpan, birBuiltIns.nothingType, value, returnTarget).apply(block)


    fun birCall(
        function: BirSimpleFunction,
        type: BirType = function.returnType,
        typeArguments: List<BirType?> = listOfNulls(function.typeParameters.size),
        origin: IrStatementOrigin? = this.origin,
        block: BirCall.() -> Unit = {},
    ): BirCall =
        BirCallImpl(
            sourceSpan = sourceSpan,
            type = type,
            symbol = function,
            dispatchReceiver = null,
            extensionReceiver = null,
            origin = origin,
            typeArguments = typeArguments,
            contextReceiversCount = 0,
            superQualifierSymbol = null
        ).apply {
            valueArguments.resetWithNulls(function.valueParameters.size)
            block()
        }

    fun birCall(
        constructor: BirConstructor,
        type: BirType = constructor.returnType,
        typeArguments: List<BirType?> = listOfNulls(constructor.constructedClass.typeParameters.size + constructor.typeParameters.size),
        origin: IrStatementOrigin? = this.origin,
        block: BirConstructorCall.() -> Unit = {},
    ): BirConstructorCall =
        BirConstructorCallImpl(
            sourceSpan = sourceSpan,
            type = type,
            symbol = constructor,
            dispatchReceiver = null,
            extensionReceiver = null,
            origin = origin,
            typeArguments = typeArguments,
            contextReceiversCount = 0,
            source = SourceElement.NO_SOURCE,
            constructorTypeArgumentsCount = 0
        ).apply {
            valueArguments.resetWithNulls(constructor.valueParameters.size)
            block()
        }

    fun birCallFunctionOrConstructor(
        callee: BirFunctionSymbol,
        type: BirType = callee.owner.returnType,
        origin: IrStatementOrigin? = this.origin,
        block: BirFunctionAccessExpression.() -> Unit = {},
    ): BirFunctionAccessExpression = when (callee) {
        is BirConstructorSymbol -> birCall(callee.owner, type, origin = origin).apply(block)
        is BirSimpleFunctionSymbol -> birCall(callee.owner, type, origin = origin).apply(block)
        else -> error("Unhandled symbol type: " + callee.javaClass)
    }

    fun birDelegatingConstructorCall(
        constructor: BirConstructor,
        origin: IrStatementOrigin? = this.origin,
        block: BirDelegatingConstructorCall.() -> Unit = {},
    ): BirDelegatingConstructorCall =
        BirDelegatingConstructorCallImpl(
            sourceSpan = sourceSpan,
            type = birBuiltIns.unitType,
            symbol = constructor,
            dispatchReceiver = null,
            extensionReceiver = null,
            origin = origin,
            typeArguments = listOfNulls(constructor.constructedClass.typeParameters.size),
            contextReceiversCount = 0,
        )

    fun birCallGetter(
        property: BirProperty,
        type: BirType = property.getter!!.returnType,
        origin: IrStatementOrigin? = this.origin,
        block: BirCall.() -> Unit = {},
    ): BirCall =
        BirCallImpl(
            sourceSpan = sourceSpan,
            type = type,
            symbol = property.getter!!,
            dispatchReceiver = null,
            extensionReceiver = null,
            origin = origin,
            typeArguments = emptyList(),
            contextReceiversCount = 0,
            superQualifierSymbol = null,
        ).apply(block)

    fun birCallGetter(
        getter: BirSimpleFunctionSymbol,
        type: BirType = getter.owner.returnType,
        origin: IrStatementOrigin? = this.origin,
        block: BirCall.() -> Unit = {},
    ): BirCall =
        BirCallImpl(
            sourceSpan = sourceSpan,
            type = type,
            symbol = getter,
            dispatchReceiver = null,
            extensionReceiver = null,
            origin = origin,
            typeArguments = emptyList(),
            contextReceiversCount = 0,
            superQualifierSymbol = null,
        ).apply(block)

    fun birCallSetter(
        property: BirProperty,
        value: BirExpression,
        type: BirType = property.setter!!.returnType,
        origin: IrStatementOrigin? = this.origin,
        block: BirCall.() -> Unit = {},
    ): BirCall =
        BirCallImpl(
            sourceSpan = sourceSpan,
            type = type,
            symbol = property.setter!!,
            dispatchReceiver = null,
            extensionReceiver = null,
            origin = origin,
            typeArguments = emptyList(),
            contextReceiversCount = 0,
            superQualifierSymbol = null
        ).apply {
            valueArguments += value
        }


    fun birFunctionReference(
        function: BirFunction,
        type: BirType,
        typeArguments: List<BirType?> = listOfNulls(function.typeParameters.size),
        origin: IrStatementOrigin? = this.origin,
        block: BirFunctionReference.() -> Unit = {},
    ): BirFunctionReference =
        BirFunctionReferenceImpl(sourceSpan, type, null, null, origin, typeArguments, function, null).apply {
            valueArguments.resetWithNulls(function.valueParameters.size)
            block()
        }

    fun birRafFunctionReference(
        function: BirFunction,
        type: BirType,
        block: BirRawFunctionReference.() -> Unit = {},
    ): BirRawFunctionReference =
        BirRawFunctionReferenceImpl(sourceSpan, type, function).apply(block)


    fun birTypeOperator(
        argument: BirExpression,
        resultType: BirType,
        typeOperator: IrTypeOperator,
        typeOperand: BirType,
        block: BirTypeOperatorCall.() -> Unit = {},
    ): BirTypeOperatorCall =
        BirTypeOperatorCallImpl(sourceSpan, resultType, typeOperator, argument, typeOperand).apply(block)

    fun birIs(
        argument: BirExpression,
        type: BirType,
        block: BirTypeOperatorCall.() -> Unit = {},
    ): BirTypeOperatorCall =
        BirTypeOperatorCallImpl(sourceSpan, birBuiltIns.booleanType, IrTypeOperator.INSTANCEOF, argument, type).apply(block)

    fun birNotIs(
        argument: BirExpression,
        type: BirType,
        block: BirTypeOperatorCall.() -> Unit = {},
    ): BirTypeOperatorCall =
        BirTypeOperatorCallImpl(sourceSpan, birBuiltIns.booleanType, IrTypeOperator.NOT_INSTANCEOF, argument, type).apply(block)

    fun birCast(
        argument: BirExpression,
        targetType: BirType,
        castType: IrTypeOperator = IrTypeOperator.CAST,
        block: BirTypeOperatorCall.() -> Unit = {},
    ): BirTypeOperatorCall =
        BirTypeOperatorCallImpl(sourceSpan, targetType, castType, argument, targetType).apply(block)

    fun birSamConversion(
        argument: BirExpression,
        targetType: BirType,
        block: BirTypeOperatorCall.() -> Unit = {},
    ): BirTypeOperatorCall =
        BirTypeOperatorCallImpl(sourceSpan, targetType, IrTypeOperator.SAM_CONVERSION, argument, targetType).apply(block)


    fun birConcat(
        block: BirStringConcatenation.() -> Unit = {},
    ): BirStringConcatenation =
        BirStringConcatenationImpl(sourceSpan, birBuiltIns.stringType).apply(block)

    fun birVararg(
        elementType: BirType,
        block: BirVararg.() -> Unit = {},
    ): BirVararg =
        BirVarargImpl(sourceSpan, birBuiltIns.arrayClass.typeWith(elementType), elementType).apply(block)


    fun inventIndexedNameForTemporary(prefix: String = "tmp", nameHint: String? = null): String {
        val index = nextTemporaryIndex()
        return if (nameHint != null) "$prefix${index}_$nameHint" else "$prefix$index"
    }

    private fun getNameForTemporary(nameHint: String?, addIndexToName: Boolean): String =
        if (addIndexToName) inventIndexedNameForTemporary("tmp", nameHint)
        else nameHint ?: "tmp"

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    fun birTemporaryVariable(
        type: BirType,
        isMutable: Boolean = false,
        nameHint: String? = null,
        addIndexToName: Boolean = true,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
        sourceSpan: SourceSpan = this.sourceSpan,
        block: BirVariable.() -> Unit = {},
    ): BirVariable {
        val name = Name.identifier(getNameForTemporary(nameHint, addIndexToName))
        return BirVariableImpl(
            sourceSpan = sourceSpan, signature = null, origin = origin, name = name,
            type = type, isVar = isMutable, isAssignable = true, isConst = false, isLateinit = false, initializer = null, descriptor = null
        ).apply(block)
    }

    fun birTemporaryVariable(
        initializer: BirExpression,
        type: BirType = initializer.type,
        isMutable: Boolean = false,
        origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
        nameHint: String? = null,
        addIndexToName: Boolean = true,
        sourceSpan: SourceSpan = initializer.sourceSpan,
        block: BirVariable.() -> Unit = {},
    ): BirVariable {
        return birTemporaryVariable(type, isMutable, nameHint, addIndexToName, origin, sourceSpan) {
            this.initializer = initializer
            block()
        }
    }


    fun birEquals(
        arg1: BirExpression,
        arg2: BirExpression,
        origin: IrStatementOrigin? = IrStatementOrigin.EQEQ,
        block: BirCall.() -> Unit = {},
    ): BirCall =
        BirCallImpl(
            sourceSpan = sourceSpan,
            type = birBuiltIns.booleanType,
            symbol = birBuiltIns.eqeqSymbol,
            dispatchReceiver = null,
            extensionReceiver = null,
            origin = origin,
            typeArguments = emptyList(),
            contextReceiversCount = 0,
            superQualifierSymbol = null
        ).apply {
            valueArguments += arg1
            valueArguments += arg2
            block()
        }

    fun birNotEquals(
        arg1: BirExpression,
        arg2: BirExpression,
        origin: IrStatementOrigin? = IrStatementOrigin.EXCLEQ,
        block: BirCall.() -> Unit = {},
    ): BirCall =
        BirCallImpl(
            sourceSpan = sourceSpan,
            type = birBuiltIns.booleanType,
            symbol = birBuiltIns.booleanNotSymbol,
            dispatchReceiver = birEquals(arg1, arg2, origin = IrStatementOrigin.EXCLEQ),
            extensionReceiver = null,
            origin = origin,
            typeArguments = emptyList(),
            contextReceiversCount = 0,
            superQualifierSymbol = null
        ).apply(block)


    fun birExpressionBody(
        expression: BirExpression,
    ): BirExpressionBody =
        BirExpressionBodyImpl(sourceSpan, expression)
}

context(BirStatementBuilderScope)
fun birIfThenElse(
    type: BirType,
    condition: BirExpression,
    thenPart: BirExpression,
    elsePart: BirExpression,
    origin: IrStatementOrigin? = null,
    block: BirBranch.() -> Unit = {},
) = birWhen(type, origin) {
    branches += birBranch(condition, thenPart)
    branches += birElseBranch(elsePart)
}