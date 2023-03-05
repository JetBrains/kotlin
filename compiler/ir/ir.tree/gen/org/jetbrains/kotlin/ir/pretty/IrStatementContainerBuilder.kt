/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.pretty


@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irConstructorCall(block: IrElementBuilderClosure<IrConstructorCallBuilder>) {
    __internal_addStatementBuilder(IrConstructorCallBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irGetObjectValue(block: IrElementBuilderClosure<IrGetObjectValueBuilder>) {
    __internal_addStatementBuilder(IrGetObjectValueBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irGetEnumValue(block: IrElementBuilderClosure<IrGetEnumValueBuilder>) {
    __internal_addStatementBuilder(IrGetEnumValueBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irRawFunctionReference(block: IrElementBuilderClosure<IrRawFunctionReferenceBuilder>) {
    __internal_addStatementBuilder(IrRawFunctionReferenceBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irComposite(block: IrElementBuilderClosure<IrCompositeBuilder>) {
    __internal_addStatementBuilder(IrCompositeBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irReturnableBlock(block: IrElementBuilderClosure<IrReturnableBlockBuilder>) {
    __internal_addStatementBuilder(IrReturnableBlockBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irBreak(block: IrElementBuilderClosure<IrBreakBuilder>) {
    __internal_addStatementBuilder(IrBreakBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irContinue(block: IrElementBuilderClosure<IrContinueBuilder>) {
    __internal_addStatementBuilder(IrContinueBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrStatementContainerBuilder.irCall(block: IrElementBuilderClosure<IrCallBuilder>) {
    __internal_addStatementBuilder(IrCallBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irFunctionReference(block: IrElementBuilderClosure<IrFunctionReferenceBuilder>) {
    __internal_addStatementBuilder(IrFunctionReferenceBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irPropertyReference(block: IrElementBuilderClosure<IrPropertyReferenceBuilder>) {
    __internal_addStatementBuilder(IrPropertyReferenceBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irLocalDelegatedPropertyReference(block: IrElementBuilderClosure<IrLocalDelegatedPropertyReferenceBuilder>) {
    __internal_addStatementBuilder(IrLocalDelegatedPropertyReferenceBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irClassReference(block: IrElementBuilderClosure<IrClassReferenceBuilder>) {
    __internal_addStatementBuilder(IrClassReferenceBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun <T>
        IrStatementContainerBuilder.irConst(block: IrElementBuilderClosure<IrConstBuilder<T>>) {
    __internal_addStatementBuilder(IrConstBuilder<T>(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irConstantPrimitive(block: IrElementBuilderClosure<IrConstantPrimitiveBuilder>) {
    __internal_addStatementBuilder(IrConstantPrimitiveBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irConstantObject(block: IrElementBuilderClosure<IrConstantObjectBuilder>) {
    __internal_addStatementBuilder(IrConstantObjectBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irConstantArray(block: IrElementBuilderClosure<IrConstantArrayBuilder>) {
    __internal_addStatementBuilder(IrConstantArrayBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irDelegatingConstructorCall(block: IrElementBuilderClosure<IrDelegatingConstructorCallBuilder>) {
    __internal_addStatementBuilder(IrDelegatingConstructorCallBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irDynamicOperatorExpression(block: IrElementBuilderClosure<IrDynamicOperatorExpressionBuilder>) {
    __internal_addStatementBuilder(IrDynamicOperatorExpressionBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irDynamicMemberExpression(block: IrElementBuilderClosure<IrDynamicMemberExpressionBuilder>) {
    __internal_addStatementBuilder(IrDynamicMemberExpressionBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irEnumConstructorCall(block: IrElementBuilderClosure<IrEnumConstructorCallBuilder>) {
    __internal_addStatementBuilder(IrEnumConstructorCallBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irErrorCallExpression(block: IrElementBuilderClosure<IrErrorCallExpressionBuilder>) {
    __internal_addStatementBuilder(IrErrorCallExpressionBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irGetField(block: IrElementBuilderClosure<IrGetFieldBuilder>) {
    __internal_addStatementBuilder(IrGetFieldBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irSetField(block: IrElementBuilderClosure<IrSetFieldBuilder>) {
    __internal_addStatementBuilder(IrSetFieldBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irFunctionExpression(block: IrElementBuilderClosure<IrFunctionExpressionBuilder>) {
    __internal_addStatementBuilder(IrFunctionExpressionBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irGetClass(block: IrElementBuilderClosure<IrGetClassBuilder>) {
    __internal_addStatementBuilder(IrGetClassBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irInstanceInitializerCall(block: IrElementBuilderClosure<IrInstanceInitializerCallBuilder>) {
    __internal_addStatementBuilder(IrInstanceInitializerCallBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irWhileLoop(block: IrElementBuilderClosure<IrWhileLoopBuilder>) {
    __internal_addStatementBuilder(IrWhileLoopBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irDoWhileLoop(block: IrElementBuilderClosure<IrDoWhileLoopBuilder>) {
    __internal_addStatementBuilder(IrDoWhileLoopBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irReturn(block: IrElementBuilderClosure<IrReturnBuilder>) {
    __internal_addStatementBuilder(IrReturnBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irStringConcatenation(block: IrElementBuilderClosure<IrStringConcatenationBuilder>) {
    __internal_addStatementBuilder(IrStringConcatenationBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irSuspensionPoint(block: IrElementBuilderClosure<IrSuspensionPointBuilder>) {
    __internal_addStatementBuilder(IrSuspensionPointBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irSuspendableExpression(block: IrElementBuilderClosure<IrSuspendableExpressionBuilder>) {
    __internal_addStatementBuilder(IrSuspendableExpressionBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irThrow(block: IrElementBuilderClosure<IrThrowBuilder>) {
    __internal_addStatementBuilder(IrThrowBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrStatementContainerBuilder.irTry(block: IrElementBuilderClosure<IrTryBuilder>) {
    __internal_addStatementBuilder(IrTryBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irTypeOperatorCall(block: IrElementBuilderClosure<IrTypeOperatorCallBuilder>) {
    __internal_addStatementBuilder(IrTypeOperatorCallBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irGetValue(block: IrElementBuilderClosure<IrGetValueBuilder>) {
    __internal_addStatementBuilder(IrGetValueBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irSetValue(block: IrElementBuilderClosure<IrSetValueBuilder>) {
    __internal_addStatementBuilder(IrSetValueBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrStatementContainerBuilder.irVararg(block: IrElementBuilderClosure<IrVarargBuilder>) {
    __internal_addStatementBuilder(IrVarargBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrStatementContainerBuilder.irWhen(block: IrElementBuilderClosure<IrWhenBuilder>) {
    __internal_addStatementBuilder(IrWhenBuilder(buildingContext).apply(block))
}
