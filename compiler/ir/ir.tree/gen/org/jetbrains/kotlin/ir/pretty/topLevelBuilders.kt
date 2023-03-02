/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrErrorDeclaration
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunctionWithLateBinding
import org.jetbrains.kotlin.ir.declarations.IrLocalDelegatedProperty
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrPropertyWithLateBinding
import org.jetbrains.kotlin.ir.declarations.IrScript
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeAlias
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBreak
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrCatch
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrComposite
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstantArray
import org.jetbrains.kotlin.ir.expressions.IrConstantObject
import org.jetbrains.kotlin.ir.expressions.IrConstantPrimitive
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrContinue
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDoWhileLoop
import org.jetbrains.kotlin.ir.expressions.IrDynamicMemberExpression
import org.jetbrains.kotlin.ir.expressions.IrDynamicOperatorExpression
import org.jetbrains.kotlin.ir.expressions.IrElseBranch
import org.jetbrains.kotlin.ir.expressions.IrEnumConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrErrorCallExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrGetClass
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrInstanceInitializerCall
import org.jetbrains.kotlin.ir.expressions.IrLocalDelegatedPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrRawFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrSpreadElement
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.expressions.IrSuspendableExpression
import org.jetbrains.kotlin.ir.expressions.IrSuspensionPoint
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBody
import org.jetbrains.kotlin.ir.expressions.IrThrow
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop

fun buildIrValueParameter(
    name: String,
    buildingContext: IrBuildingContext = IrBuildingContext(),
    block: IrElementBuilderClosure<IrValueParameterBuilder>,
): IrValueParameter = IrValueParameterBuilder(name, buildingContext).apply(block).build()

fun buildIrClass(
    name: String,
    buildingContext: IrBuildingContext = IrBuildingContext(),
    block: IrElementBuilderClosure<IrClassBuilder>,
): IrClass = IrClassBuilder(name, buildingContext).apply(block).build()

fun buildIrAnonymousInitializer(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrAnonymousInitializerBuilder>): IrAnonymousInitializer =
        IrAnonymousInitializerBuilder(buildingContext).apply(block).build()

fun buildIrTypeParameter(
    name: String,
    buildingContext: IrBuildingContext = IrBuildingContext(),
    block: IrElementBuilderClosure<IrTypeParameterBuilder>,
): IrTypeParameter = IrTypeParameterBuilder(name, buildingContext).apply(block).build()

fun buildIrConstructor(
    name: String,
    buildingContext: IrBuildingContext = IrBuildingContext(),
    block: IrElementBuilderClosure<IrConstructorBuilder>,
): IrConstructor = IrConstructorBuilder(name, buildingContext).apply(block).build()

fun buildIrEnumEntry(
    name: String,
    buildingContext: IrBuildingContext = IrBuildingContext(),
    block: IrElementBuilderClosure<IrEnumEntryBuilder>,
): IrEnumEntry = IrEnumEntryBuilder(name, buildingContext).apply(block).build()

fun buildIrErrorDeclaration(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrErrorDeclarationBuilder>): IrErrorDeclaration =
        IrErrorDeclarationBuilder(buildingContext).apply(block).build()

fun buildIrFunctionWithLateBinding(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrFunctionWithLateBindingBuilder>): IrFunctionWithLateBinding
        = IrFunctionWithLateBindingBuilder(buildingContext).apply(block).build()

fun buildIrPropertyWithLateBinding(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrPropertyWithLateBindingBuilder>): IrPropertyWithLateBinding
        = IrPropertyWithLateBindingBuilder(buildingContext).apply(block).build()

fun buildIrField(
    name: String,
    buildingContext: IrBuildingContext = IrBuildingContext(),
    block: IrElementBuilderClosure<IrFieldBuilder>,
): IrField = IrFieldBuilder(name, buildingContext).apply(block).build()

fun buildIrLocalDelegatedProperty(
    name: String,
    buildingContext: IrBuildingContext = IrBuildingContext(),
    block: IrElementBuilderClosure<IrLocalDelegatedPropertyBuilder>,
): IrLocalDelegatedProperty = IrLocalDelegatedPropertyBuilder(name,
        buildingContext).apply(block).build()

fun buildIrModuleFragment(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrModuleFragmentBuilder>): IrModuleFragment =
        IrModuleFragmentBuilder(buildingContext).apply(block).build()

fun buildIrProperty(
    name: String,
    buildingContext: IrBuildingContext = IrBuildingContext(),
    block: IrElementBuilderClosure<IrPropertyBuilder>,
): IrProperty = IrPropertyBuilder(name, buildingContext).apply(block).build()

fun buildIrScript(
    name: String,
    buildingContext: IrBuildingContext = IrBuildingContext(),
    block: IrElementBuilderClosure<IrScriptBuilder>,
): IrScript = IrScriptBuilder(name, buildingContext).apply(block).build()

fun buildIrSimpleFunction(
    name: String,
    buildingContext: IrBuildingContext = IrBuildingContext(),
    block: IrElementBuilderClosure<IrSimpleFunctionBuilder>,
): IrSimpleFunction = IrSimpleFunctionBuilder(name, buildingContext).apply(block).build()

fun buildIrTypeAlias(
    name: String,
    buildingContext: IrBuildingContext = IrBuildingContext(),
    block: IrElementBuilderClosure<IrTypeAliasBuilder>,
): IrTypeAlias = IrTypeAliasBuilder(name, buildingContext).apply(block).build()

fun buildIrVariable(
    name: String,
    buildingContext: IrBuildingContext = IrBuildingContext(),
    block: IrElementBuilderClosure<IrVariableBuilder>,
): IrVariable = IrVariableBuilder(name, buildingContext).apply(block).build()

fun buildIrExternalPackageFragment(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrExternalPackageFragmentBuilder>): IrExternalPackageFragment
        = IrExternalPackageFragmentBuilder(buildingContext).apply(block).build()

fun buildIrFile(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrFileBuilder>): IrFile =
        IrFileBuilder(buildingContext).apply(block).build()

fun buildIrExpressionBody(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrExpressionBodyBuilder>): IrExpressionBody =
        IrExpressionBodyBuilder(buildingContext).apply(block).build()

fun buildIrBlockBody(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrBlockBodyBuilder>): IrBlockBody =
        IrBlockBodyBuilder(buildingContext).apply(block).build()

fun buildIrConstructorCall(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrConstructorCallBuilder>): IrConstructorCall =
        IrConstructorCallBuilder(buildingContext).apply(block).build()

fun buildIrGetObjectValue(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrGetObjectValueBuilder>): IrGetObjectValue =
        IrGetObjectValueBuilder(buildingContext).apply(block).build()

fun buildIrGetEnumValue(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrGetEnumValueBuilder>): IrGetEnumValue =
        IrGetEnumValueBuilder(buildingContext).apply(block).build()

fun buildIrRawFunctionReference(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrRawFunctionReferenceBuilder>): IrRawFunctionReference =
        IrRawFunctionReferenceBuilder(buildingContext).apply(block).build()

fun buildIrComposite(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrCompositeBuilder>): IrComposite =
        IrCompositeBuilder(buildingContext).apply(block).build()

fun buildIrReturnableBlock(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrReturnableBlockBuilder>): IrReturnableBlock =
        IrReturnableBlockBuilder(buildingContext).apply(block).build()

fun buildIrSyntheticBody(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrSyntheticBodyBuilder>): IrSyntheticBody =
        IrSyntheticBodyBuilder(buildingContext).apply(block).build()

fun buildIrBreak(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrBreakBuilder>): IrBreak =
        IrBreakBuilder(buildingContext).apply(block).build()

fun buildIrContinue(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrContinueBuilder>): IrContinue =
        IrContinueBuilder(buildingContext).apply(block).build()

fun buildIrCall(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrCallBuilder>): IrCall =
        IrCallBuilder(buildingContext).apply(block).build()

fun buildIrFunctionReference(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrFunctionReferenceBuilder>): IrFunctionReference =
        IrFunctionReferenceBuilder(buildingContext).apply(block).build()

fun buildIrPropertyReference(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrPropertyReferenceBuilder>): IrPropertyReference =
        IrPropertyReferenceBuilder(buildingContext).apply(block).build()

fun buildIrLocalDelegatedPropertyReference(buildingContext: IrBuildingContext =
        IrBuildingContext(),
        block: IrElementBuilderClosure<IrLocalDelegatedPropertyReferenceBuilder>):
        IrLocalDelegatedPropertyReference =
        IrLocalDelegatedPropertyReferenceBuilder(buildingContext).apply(block).build()

fun buildIrClassReference(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrClassReferenceBuilder>): IrClassReference =
        IrClassReferenceBuilder(buildingContext).apply(block).build()

fun <T> buildIrConst(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrConstBuilder<T>>): IrConst<T> =
        IrConstBuilder(buildingContext).apply(block).build()

fun buildIrConstantPrimitive(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrConstantPrimitiveBuilder>): IrConstantPrimitive =
        IrConstantPrimitiveBuilder(buildingContext).apply(block).build()

fun buildIrConstantObject(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrConstantObjectBuilder>): IrConstantObject =
        IrConstantObjectBuilder(buildingContext).apply(block).build()

fun buildIrConstantArray(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrConstantArrayBuilder>): IrConstantArray =
        IrConstantArrayBuilder(buildingContext).apply(block).build()

fun buildIrDelegatingConstructorCall(buildingContext: IrBuildingContext =
        IrBuildingContext(), block: IrElementBuilderClosure<IrDelegatingConstructorCallBuilder>):
        IrDelegatingConstructorCall =
        IrDelegatingConstructorCallBuilder(buildingContext).apply(block).build()

fun buildIrDynamicOperatorExpression(buildingContext: IrBuildingContext =
        IrBuildingContext(), block: IrElementBuilderClosure<IrDynamicOperatorExpressionBuilder>):
        IrDynamicOperatorExpression =
        IrDynamicOperatorExpressionBuilder(buildingContext).apply(block).build()

fun buildIrDynamicMemberExpression(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrDynamicMemberExpressionBuilder>): IrDynamicMemberExpression
        = IrDynamicMemberExpressionBuilder(buildingContext).apply(block).build()

fun buildIrEnumConstructorCall(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrEnumConstructorCallBuilder>): IrEnumConstructorCall =
        IrEnumConstructorCallBuilder(buildingContext).apply(block).build()

fun buildIrErrorCallExpression(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrErrorCallExpressionBuilder>): IrErrorCallExpression =
        IrErrorCallExpressionBuilder(buildingContext).apply(block).build()

fun buildIrGetField(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrGetFieldBuilder>): IrGetField =
        IrGetFieldBuilder(buildingContext).apply(block).build()

fun buildIrSetField(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrSetFieldBuilder>): IrSetField =
        IrSetFieldBuilder(buildingContext).apply(block).build()

fun buildIrFunctionExpression(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrFunctionExpressionBuilder>): IrFunctionExpression =
        IrFunctionExpressionBuilder(buildingContext).apply(block).build()

fun buildIrGetClass(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrGetClassBuilder>): IrGetClass =
        IrGetClassBuilder(buildingContext).apply(block).build()

fun buildIrInstanceInitializerCall(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrInstanceInitializerCallBuilder>): IrInstanceInitializerCall
        = IrInstanceInitializerCallBuilder(buildingContext).apply(block).build()

fun buildIrWhileLoop(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrWhileLoopBuilder>): IrWhileLoop =
        IrWhileLoopBuilder(buildingContext).apply(block).build()

fun buildIrDoWhileLoop(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrDoWhileLoopBuilder>): IrDoWhileLoop =
        IrDoWhileLoopBuilder(buildingContext).apply(block).build()

fun buildIrReturn(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrReturnBuilder>): IrReturn =
        IrReturnBuilder(buildingContext).apply(block).build()

fun buildIrStringConcatenation(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrStringConcatenationBuilder>): IrStringConcatenation =
        IrStringConcatenationBuilder(buildingContext).apply(block).build()

fun buildIrSuspensionPoint(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrSuspensionPointBuilder>): IrSuspensionPoint =
        IrSuspensionPointBuilder(buildingContext).apply(block).build()

fun buildIrSuspendableExpression(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrSuspendableExpressionBuilder>): IrSuspendableExpression =
        IrSuspendableExpressionBuilder(buildingContext).apply(block).build()

fun buildIrThrow(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrThrowBuilder>): IrThrow =
        IrThrowBuilder(buildingContext).apply(block).build()

fun buildIrTry(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrTryBuilder>): IrTry =
        IrTryBuilder(buildingContext).apply(block).build()

fun buildIrCatch(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrCatchBuilder>): IrCatch =
        IrCatchBuilder(buildingContext).apply(block).build()

fun buildIrTypeOperatorCall(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrTypeOperatorCallBuilder>): IrTypeOperatorCall =
        IrTypeOperatorCallBuilder(buildingContext).apply(block).build()

fun buildIrGetValue(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrGetValueBuilder>): IrGetValue =
        IrGetValueBuilder(buildingContext).apply(block).build()

fun buildIrSetValue(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrSetValueBuilder>): IrSetValue =
        IrSetValueBuilder(buildingContext).apply(block).build()

fun buildIrVararg(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrVarargBuilder>): IrVararg =
        IrVarargBuilder(buildingContext).apply(block).build()

fun buildIrSpreadElement(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrSpreadElementBuilder>): IrSpreadElement =
        IrSpreadElementBuilder(buildingContext).apply(block).build()

fun buildIrWhen(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrWhenBuilder>): IrWhen =
        IrWhenBuilder(buildingContext).apply(block).build()

fun buildIrElseBranch(buildingContext: IrBuildingContext = IrBuildingContext(),
        block: IrElementBuilderClosure<IrElseBranchBuilder>): IrElseBranch =
        IrElseBranchBuilder(buildingContext).apply(block).build()
