/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.pretty


@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irConstructorCall(block: IrElementBuilderClosure<IrConstructorCallBuilder>) {
    __internal_addExpressionBuilder(IrConstructorCallBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irGetObjectValue(block: IrElementBuilderClosure<IrGetObjectValueBuilder>) {
    __internal_addExpressionBuilder(IrGetObjectValueBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irGetEnumValue(block: IrElementBuilderClosure<IrGetEnumValueBuilder>) {
    __internal_addExpressionBuilder(IrGetEnumValueBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irRawFunctionReference(block: IrElementBuilderClosure<IrRawFunctionReferenceBuilder>) {
    __internal_addExpressionBuilder(IrRawFunctionReferenceBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irComposite(block: IrElementBuilderClosure<IrCompositeBuilder>) {
    __internal_addExpressionBuilder(IrCompositeBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irReturnableBlock(block: IrElementBuilderClosure<IrReturnableBlockBuilder>) {
    __internal_addExpressionBuilder(IrReturnableBlockBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irBreak(block: IrElementBuilderClosure<IrBreakBuilder>) {
    __internal_addExpressionBuilder(IrBreakBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irContinue(block: IrElementBuilderClosure<IrContinueBuilder>) {
    __internal_addExpressionBuilder(IrContinueBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irCall(block: IrElementBuilderClosure<IrCallBuilder>) {
    __internal_addExpressionBuilder(IrCallBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irFunctionReference(block: IrElementBuilderClosure<IrFunctionReferenceBuilder>) {
    __internal_addExpressionBuilder(IrFunctionReferenceBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irPropertyReference(block: IrElementBuilderClosure<IrPropertyReferenceBuilder>) {
    __internal_addExpressionBuilder(IrPropertyReferenceBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irLocalDelegatedPropertyReference(block: IrElementBuilderClosure<IrLocalDelegatedPropertyReferenceBuilder>) {
    __internal_addExpressionBuilder(IrLocalDelegatedPropertyReferenceBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irClassReference(block: IrElementBuilderClosure<IrClassReferenceBuilder>) {
    __internal_addExpressionBuilder(IrClassReferenceBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun <T>
        IrExpressionContainerBuilder.irConst(block: IrElementBuilderClosure<IrConstBuilder<T>>) {
    __internal_addExpressionBuilder(IrConstBuilder<T>(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irConstantPrimitive(block: IrElementBuilderClosure<IrConstantPrimitiveBuilder>) {
    __internal_addExpressionBuilder(IrConstantPrimitiveBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irConstantObject(block: IrElementBuilderClosure<IrConstantObjectBuilder>) {
    __internal_addExpressionBuilder(IrConstantObjectBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irConstantArray(block: IrElementBuilderClosure<IrConstantArrayBuilder>) {
    __internal_addExpressionBuilder(IrConstantArrayBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irDelegatingConstructorCall(block: IrElementBuilderClosure<IrDelegatingConstructorCallBuilder>) {
    __internal_addExpressionBuilder(IrDelegatingConstructorCallBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irDynamicOperatorExpression(block: IrElementBuilderClosure<IrDynamicOperatorExpressionBuilder>) {
    __internal_addExpressionBuilder(IrDynamicOperatorExpressionBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irDynamicMemberExpression(block: IrElementBuilderClosure<IrDynamicMemberExpressionBuilder>) {
    __internal_addExpressionBuilder(IrDynamicMemberExpressionBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irEnumConstructorCall(block: IrElementBuilderClosure<IrEnumConstructorCallBuilder>) {
    __internal_addExpressionBuilder(IrEnumConstructorCallBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irErrorCallExpression(block: IrElementBuilderClosure<IrErrorCallExpressionBuilder>) {
    __internal_addExpressionBuilder(IrErrorCallExpressionBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irGetField(block: IrElementBuilderClosure<IrGetFieldBuilder>) {
    __internal_addExpressionBuilder(IrGetFieldBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irSetField(block: IrElementBuilderClosure<IrSetFieldBuilder>) {
    __internal_addExpressionBuilder(IrSetFieldBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irFunctionExpression(block: IrElementBuilderClosure<IrFunctionExpressionBuilder>) {
    __internal_addExpressionBuilder(IrFunctionExpressionBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irGetClass(block: IrElementBuilderClosure<IrGetClassBuilder>) {
    __internal_addExpressionBuilder(IrGetClassBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irInstanceInitializerCall(block: IrElementBuilderClosure<IrInstanceInitializerCallBuilder>) {
    __internal_addExpressionBuilder(IrInstanceInitializerCallBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irWhileLoop(block: IrElementBuilderClosure<IrWhileLoopBuilder>) {
    __internal_addExpressionBuilder(IrWhileLoopBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irDoWhileLoop(block: IrElementBuilderClosure<IrDoWhileLoopBuilder>) {
    __internal_addExpressionBuilder(IrDoWhileLoopBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irReturn(block: IrElementBuilderClosure<IrReturnBuilder>) {
    __internal_addExpressionBuilder(IrReturnBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irStringConcatenation(block: IrElementBuilderClosure<IrStringConcatenationBuilder>) {
    __internal_addExpressionBuilder(IrStringConcatenationBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irSuspensionPoint(block: IrElementBuilderClosure<IrSuspensionPointBuilder>) {
    __internal_addExpressionBuilder(IrSuspensionPointBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irSuspendableExpression(block: IrElementBuilderClosure<IrSuspendableExpressionBuilder>) {
    __internal_addExpressionBuilder(IrSuspendableExpressionBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irThrow(block: IrElementBuilderClosure<IrThrowBuilder>) {
    __internal_addExpressionBuilder(IrThrowBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline fun IrExpressionContainerBuilder.irTry(block: IrElementBuilderClosure<IrTryBuilder>) {
    __internal_addExpressionBuilder(IrTryBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irTypeOperatorCall(block: IrElementBuilderClosure<IrTypeOperatorCallBuilder>) {
    __internal_addExpressionBuilder(IrTypeOperatorCallBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irGetValue(block: IrElementBuilderClosure<IrGetValueBuilder>) {
    __internal_addExpressionBuilder(IrGetValueBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irSetValue(block: IrElementBuilderClosure<IrSetValueBuilder>) {
    __internal_addExpressionBuilder(IrSetValueBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irVararg(block: IrElementBuilderClosure<IrVarargBuilder>) {
    __internal_addExpressionBuilder(IrVarargBuilder(buildingContext).apply(block))
}

@IrNodeBuilderDsl
inline
        fun IrExpressionContainerBuilder.irWhen(block: IrElementBuilderClosure<IrWhenBuilder>) {
    __internal_addExpressionBuilder(IrWhenBuilder(buildingContext).apply(block))
}
