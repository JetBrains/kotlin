/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.nextgen

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor as DescriptorReferenceMessageType
import org.jetbrains.kotlin.backend.common.serialization.UniqId as UniqIdMessageType
import org.jetbrains.kotlin.backend.common.serialization.nextgen.CoordinatesCarrier as CoordinatesMessageType
import org.jetbrains.kotlin.descriptors.Visibility as VisibilityMessageType
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin as IrStatementOriginMessageType
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin as KnownOriginMessageType
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin as IrDeclarationOriginMessageType
import kotlin.Int
import org.jetbrains.kotlin.name.FqName as FqNameMessageType
import kotlin.collections.List
import org.jetbrains.kotlin.ir.SourceManager.FileEntry as FileEntryMessageType
import org.jetbrains.kotlin.ir.declarations.IrFile as IrFileMessageType
import kotlin.Array
import org.jetbrains.kotlin.ir.symbols.IrSymbol as IrSymbolDataMessageType
import org.jetbrains.kotlin.types.Variance as IrTypeVarianceMessageType
import org.jetbrains.kotlin.ir.types.IrStarProjection as IrStarProjectionMessageType
import org.jetbrains.kotlin.ir.types.IrTypeProjection as IrTypeProjectionMessageType
import org.jetbrains.kotlin.ir.types.IrTypeArgument as IrTypeArgumentMessageType
import org.jetbrains.kotlin.ir.types.IrSimpleType as IrSimpleTypeMessageType
import org.jetbrains.kotlin.ir.types.IrTypeAbbreviation as IrTypeAbbreviationMessageType
import org.jetbrains.kotlin.ir.types.IrDynamicType as IrDynamicTypeMessageType
import org.jetbrains.kotlin.ir.types.IrErrorType as IrErrorTypeMessageType
import org.jetbrains.kotlin.ir.types.IrType as IrTypeMessageType
import org.jetbrains.kotlin.ir.expressions.IrBreak as IrBreakMessageType
import org.jetbrains.kotlin.ir.expressions.IrBlock as IrBlockMessageType
import org.jetbrains.kotlin.backend.common.serialization.nextgen.MemberAccessCarrier as MemberAccessCommonMessageType
import org.jetbrains.kotlin.ir.expressions.IrCall as IrCallMessageType
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall as IrConstructorCallMessageType
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference as IrFunctionReferenceMessageType
import org.jetbrains.kotlin.ir.expressions.IrLocalDelegatedPropertyReference as IrLocalDelegatedPropertyReferenceMessageType
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference as IrPropertyReferenceMessageType
import org.jetbrains.kotlin.ir.expressions.IrComposite as IrCompositeMessageType
import org.jetbrains.kotlin.ir.expressions.IrClassReference as IrClassReferenceMessageType
import org.jetbrains.kotlin.ir.expressions.IrConst as IrConstMessageType
import org.jetbrains.kotlin.ir.expressions.IrContinue as IrContinueMessageType
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall as IrDelegatingConstructorCallMessageType
import org.jetbrains.kotlin.ir.expressions.IrDoWhileLoop as IrDoWhileMessageType
import org.jetbrains.kotlin.ir.expressions.IrEnumConstructorCall as IrEnumConstructorCallMessageType
import org.jetbrains.kotlin.ir.expressions.IrGetClass as IrGetClassMessageType
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue as IrGetEnumValueMessageType
import org.jetbrains.kotlin.backend.common.serialization.nextgen.FieldAccessCarrier as FieldAccessCommonMessageType
import org.jetbrains.kotlin.ir.expressions.IrGetField as IrGetFieldMessageType
import org.jetbrains.kotlin.ir.expressions.IrGetValue as IrGetValueMessageType
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue as IrGetObjectMessageType
import org.jetbrains.kotlin.ir.expressions.IrInstanceInitializerCall as IrInstanceInitializerCallMessageType
import org.jetbrains.kotlin.backend.common.serialization.nextgen.LoopCarrier as LoopMessageType
import org.jetbrains.kotlin.ir.expressions.IrReturn as IrReturnMessageType
import org.jetbrains.kotlin.ir.expressions.IrSetField as IrSetFieldMessageType
import org.jetbrains.kotlin.ir.expressions.IrSetVariable as IrSetVariableMessageType
import org.jetbrains.kotlin.ir.expressions.IrSpreadElement as IrSpreadElementMessageType
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation as IrStringConcatMessageType
import org.jetbrains.kotlin.ir.expressions.IrThrow as IrThrowMessageType
import org.jetbrains.kotlin.ir.expressions.IrTry as IrTryMessageType
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall as IrTypeOpMessageType
import org.jetbrains.kotlin.ir.expressions.IrVararg as IrVarargMessageType
import org.jetbrains.kotlin.ir.expressions.IrVarargElement as IrVarargElementMessageType
import org.jetbrains.kotlin.ir.expressions.IrWhen as IrWhenMessageType
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop as IrWhileMessageType
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression as IrFunctionExpressionMessageType
import org.jetbrains.kotlin.ir.expressions.IrDynamicMemberExpression as IrDynamicMemberExpressionMessageType
import org.jetbrains.kotlin.ir.expressions.IrDynamicOperator as IrDynamicOperatorMessageType
import org.jetbrains.kotlin.ir.expressions.IrDynamicOperatorExpression as IrDynamicOperatorExpressionMessageType
import org.jetbrains.kotlin.ir.expressions.IrExpression as IrOperationMessageType
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator as IrTypeOperatorMessageType
import org.jetbrains.kotlin.ir.expressions.IrExpression as IrExpressionMessageType
import org.jetbrains.kotlin.ir.expressions.IrExpression as NullableIrExpressionMessageType
import org.jetbrains.kotlin.backend.common.serialization.nextgen.DeclarationBaseCarrier as IrDeclarationBaseMessageType
import org.jetbrains.kotlin.backend.common.serialization.nextgen.FunctionBaseCarrier as IrFunctionBaseMessageType
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction as IrFunctionMessageType
import org.jetbrains.kotlin.ir.declarations.IrConstructor as IrConstructorMessageType
import org.jetbrains.kotlin.ir.declarations.IrField as IrFieldMessageType
import org.jetbrains.kotlin.ir.declarations.IrLocalDelegatedProperty as IrLocalDelegatedPropertyMessageType
import org.jetbrains.kotlin.ir.declarations.IrProperty as IrPropertyMessageType
import org.jetbrains.kotlin.ir.declarations.IrVariable as IrVariableMessageType
import org.jetbrains.kotlin.descriptors.ClassKind as ClassKindMessageType
import org.jetbrains.kotlin.descriptors.Modality as ModalityKindMessageType
import org.jetbrains.kotlin.ir.declarations.IrValueParameter as IrValueParameterMessageType
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter as IrTypeParameterMessageType
import org.jetbrains.kotlin.ir.declarations.IrClass as IrClassMessageType
import org.jetbrains.kotlin.ir.declarations.IrTypeAlias as IrTypeAliasMessageType
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry as IrEnumEntryMessageType
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer as IrAnonymousInitMessageType
import org.jetbrains.kotlin.ir.declarations.IrDeclaration as IrDeclarationMessageType
import org.jetbrains.kotlin.ir.expressions.IrBranch as IrBranchMessageType
import org.jetbrains.kotlin.ir.expressions.IrBlockBody as IrBlockBodyMessageType
import org.jetbrains.kotlin.ir.expressions.IrCatch as IrCatchMessageType
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBodyKind as IrSyntheticBodyKindMessageType
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBodyKind as IrSyntheticBodyMessageType
import org.jetbrains.kotlin.ir.IrStatement as IrStatementMessageType

abstract class AbstractIrSmartProtoReader(source: ByteArray) : ProtoReader(source) {
    abstract fun createDescriptorReference(packageFqName : FqNameMessageType, classFqName : FqNameMessageType, name : Int, uniqId : UniqIdMessageType?, isGetter : Boolean?, isSetter : Boolean?, isBackingField : Boolean?, isFakeOverride : Boolean?, isDefaultConstructor : Boolean?, isEnumEntry : Boolean?, isEnumSpecial : Boolean?, isTypeParameter : Boolean?): DescriptorReferenceMessageType

    abstract fun createUniqId(index : Long, isLocal : Boolean): UniqIdMessageType

    abstract fun createCoordinates(startOffset : Int, endOffset : Int): CoordinatesMessageType

    abstract fun createVisibility(name : Int): VisibilityMessageType

    abstract fun createIrStatementOrigin(name : Int): IrStatementOriginMessageType

    abstract fun createKnownOrigin(index: Int): KnownOriginMessageType

    abstract fun createIrDeclarationOrigin_origin(origin : KnownOriginMessageType?): IrDeclarationOriginMessageType
    abstract fun createIrDeclarationOrigin_custom(custom : Int?): IrDeclarationOriginMessageType

    abstract fun createIrDataIndex(index : Int): Int

    abstract fun createFqName(segment : List<Int>): FqNameMessageType

    abstract fun createIrDeclarationContainer(declaration : List<IrDeclarationMessageType>): List<org.jetbrains.kotlin.ir.declarations.IrDeclaration>

    abstract fun createFileEntry(name : String, lineStartOffsets : List<Int>): FileEntryMessageType

    abstract fun createIrFile(declarationId : List<UniqIdMessageType>, fileEntry : FileEntryMessageType, fqName : FqNameMessageType, annotations : List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>, explicitlyExportedToCompiler : List<Int>): IrFileMessageType

    abstract fun createStringTable(strings : List<String>): Array<String>

    abstract fun createIrSymbolKind(index: Int): Int

    abstract fun createIrSymbolData(kind : Int, uniqId : UniqIdMessageType, topLevelUniqId : UniqIdMessageType, fqname : FqNameMessageType?, descriptorReference : DescriptorReferenceMessageType?): IrSymbolDataMessageType

    abstract fun createIrSymbolTable(symbols : List<IrSymbolDataMessageType>): Array<org.jetbrains.kotlin.ir.symbols.IrSymbol>

    abstract fun createIrTypeVariance(index: Int): IrTypeVarianceMessageType

    abstract fun createAnnotations(annotation : List<IrConstructorCallMessageType>): List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>

    abstract fun createTypeArguments(typeArgument : List<Int>): List<org.jetbrains.kotlin.ir.types.IrType>

    abstract fun createIrStarProjection(void : Boolean?): IrStarProjectionMessageType

    abstract fun createIrTypeProjection(variance : IrTypeVarianceMessageType, type : Int): IrTypeProjectionMessageType

    abstract fun createIrTypeArgument_star(star : IrStarProjectionMessageType?): IrTypeArgumentMessageType
    abstract fun createIrTypeArgument_type(type : IrTypeProjectionMessageType?): IrTypeArgumentMessageType

    abstract fun createIrSimpleType(annotations : List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>, classifier : Int, hasQuestionMark : Boolean, argument : List<IrTypeArgumentMessageType>, abbreviation : IrTypeAbbreviationMessageType?): IrSimpleTypeMessageType

    abstract fun createIrTypeAbbreviation(annotations : List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>, typeAlias : Int, hasQuestionMark : Boolean, argument : List<IrTypeArgumentMessageType>): IrTypeAbbreviationMessageType

    abstract fun createIrDynamicType(annotations : List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>): IrDynamicTypeMessageType

    abstract fun createIrErrorType(annotations : List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>): IrErrorTypeMessageType

    abstract fun createIrType_simple(simple : IrSimpleTypeMessageType?): IrTypeMessageType
    abstract fun createIrType_dynamic(dynamic : IrDynamicTypeMessageType?): IrTypeMessageType
    abstract fun createIrType_error(error : IrErrorTypeMessageType?): IrTypeMessageType

    abstract fun createIrTypeTable(types : List<IrTypeMessageType>): Array<org.jetbrains.kotlin.ir.types.IrType>

    abstract fun createIrBreak(loopId : Int, label : Int?): IrBreakMessageType

    abstract fun createIrBlock(origin : IrStatementOriginMessageType?, statement : List<IrStatementMessageType>): IrBlockMessageType

    abstract fun createMemberAccessCommon(dispatchReceiver : IrExpressionMessageType?, extensionReceiver : IrExpressionMessageType?, valueArgument : List<NullableIrExpressionMessageType>, typeArguments : List<org.jetbrains.kotlin.ir.types.IrType>): MemberAccessCommonMessageType

    abstract fun createIrCall(symbol : Int, memberAccess : MemberAccessCommonMessageType, super_ : Int?, origin : IrStatementOriginMessageType?): IrCallMessageType

    abstract fun createIrConstructorCall(symbol : Int, constructorTypeArgumentsCount : Int, memberAccess : MemberAccessCommonMessageType): IrConstructorCallMessageType

    abstract fun createIrFunctionReference(symbol : Int, origin : IrStatementOriginMessageType?, memberAccess : MemberAccessCommonMessageType): IrFunctionReferenceMessageType

    abstract fun createIrLocalDelegatedPropertyReference(delegate : Int, getter : Int?, setter : Int?, symbol : Int, origin : IrStatementOriginMessageType?): IrLocalDelegatedPropertyReferenceMessageType

    abstract fun createIrPropertyReference(field : Int?, getter : Int?, setter : Int?, origin : IrStatementOriginMessageType?, memberAccess : MemberAccessCommonMessageType, symbol : Int): IrPropertyReferenceMessageType

    abstract fun createIrComposite(statement : List<IrStatementMessageType>, origin : IrStatementOriginMessageType?): IrCompositeMessageType

    abstract fun createIrClassReference(classSymbol : Int, classType : Int): IrClassReferenceMessageType

    abstract fun createIrConst_null_(null_ : Boolean?): IrConstMessageType<*>
    abstract fun createIrConst_boolean(boolean : Boolean?): IrConstMessageType<*>
    abstract fun createIrConst_char(char : Int?): IrConstMessageType<*>
    abstract fun createIrConst_byte(byte : Int?): IrConstMessageType<*>
    abstract fun createIrConst_short(short : Int?): IrConstMessageType<*>
    abstract fun createIrConst_int(int : Int?): IrConstMessageType<*>
    abstract fun createIrConst_long(long : Long?): IrConstMessageType<*>
    abstract fun createIrConst_float(float : Float?): IrConstMessageType<*>
    abstract fun createIrConst_double(double : Double?): IrConstMessageType<*>
    abstract fun createIrConst_string(string : Int?): IrConstMessageType<*>

    abstract fun createIrContinue(loopId : Int, label : Int?): IrContinueMessageType

    abstract fun createIrDelegatingConstructorCall(symbol : Int, memberAccess : MemberAccessCommonMessageType): IrDelegatingConstructorCallMessageType

    abstract fun createIrDoWhile(loop : LoopMessageType): IrDoWhileMessageType

    abstract fun createIrEnumConstructorCall(symbol : Int, memberAccess : MemberAccessCommonMessageType): IrEnumConstructorCallMessageType

    abstract fun createIrGetClass(argument : IrExpressionMessageType): IrGetClassMessageType

    abstract fun createIrGetEnumValue(symbol : Int): IrGetEnumValueMessageType

    abstract fun createFieldAccessCommon(symbol : Int, super_ : Int?, receiver : IrExpressionMessageType?): FieldAccessCommonMessageType

    abstract fun createIrGetField(fieldAccess : FieldAccessCommonMessageType, origin : IrStatementOriginMessageType?): IrGetFieldMessageType

    abstract fun createIrGetValue(symbol : Int, origin : IrStatementOriginMessageType?): IrGetValueMessageType

    abstract fun createIrGetObject(symbol : Int): IrGetObjectMessageType

    abstract fun createIrInstanceInitializerCall(symbol : Int): IrInstanceInitializerCallMessageType

    abstract fun createLoop(loopId : Int, condition : IrExpressionMessageType, label : Int?, body : IrExpressionMessageType?, origin : IrStatementOriginMessageType?): LoopMessageType

    abstract fun createIrReturn(returnTarget : Int, value : IrExpressionMessageType): IrReturnMessageType

    abstract fun createIrSetField(fieldAccess : FieldAccessCommonMessageType, value : IrExpressionMessageType, origin : IrStatementOriginMessageType?): IrSetFieldMessageType

    abstract fun createIrSetVariable(symbol : Int, value : IrExpressionMessageType, origin : IrStatementOriginMessageType?): IrSetVariableMessageType

    abstract fun createIrSpreadElement(expression : IrExpressionMessageType, coordinates : CoordinatesMessageType): IrSpreadElementMessageType

    abstract fun createIrStringConcat(argument : List<IrExpressionMessageType>): IrStringConcatMessageType

    abstract fun createIrThrow(value : IrExpressionMessageType): IrThrowMessageType

    abstract fun createIrTry(result : IrExpressionMessageType, catch : List<IrStatementMessageType>, finally : IrExpressionMessageType?): IrTryMessageType

    abstract fun createIrTypeOp(operator : IrTypeOperatorMessageType, operand : Int, argument : IrExpressionMessageType): IrTypeOpMessageType

    abstract fun createIrVararg(elementType : Int, element : List<IrVarargElementMessageType>): IrVarargMessageType

    abstract fun createIrVarargElement_expression(expression : IrExpressionMessageType?): IrVarargElementMessageType
    abstract fun createIrVarargElement_spreadElement(spreadElement : IrSpreadElementMessageType?): IrVarargElementMessageType

    abstract fun createIrWhen(branch : List<IrStatementMessageType>, origin : IrStatementOriginMessageType?): IrWhenMessageType

    abstract fun createIrWhile(loop : LoopMessageType): IrWhileMessageType

    abstract fun createIrFunctionExpression(function : IrFunctionMessageType, origin : IrStatementOriginMessageType): IrFunctionExpressionMessageType

    abstract fun createIrDynamicMemberExpression(memberName : Int, receiver : IrExpressionMessageType): IrDynamicMemberExpressionMessageType

    abstract fun createIrDynamicOperator(index: Int): IrDynamicOperatorMessageType

    abstract fun createIrDynamicOperatorExpression(operator : IrDynamicOperatorMessageType, receiver : IrExpressionMessageType, argument : List<IrExpressionMessageType>): IrDynamicOperatorExpressionMessageType

    abstract fun createIrOperation_block(block : IrBlockMessageType?): IrOperationMessageType
    abstract fun createIrOperation_break_(break_ : IrBreakMessageType?): IrOperationMessageType
    abstract fun createIrOperation_call(call : IrCallMessageType?): IrOperationMessageType
    abstract fun createIrOperation_classReference(classReference : IrClassReferenceMessageType?): IrOperationMessageType
    abstract fun createIrOperation_composite(composite : IrCompositeMessageType?): IrOperationMessageType
    abstract fun createIrOperation_const(const : IrConstMessageType<*>?): IrOperationMessageType
    abstract fun createIrOperation_continue_(continue_ : IrContinueMessageType?): IrOperationMessageType
    abstract fun createIrOperation_delegatingConstructorCall(delegatingConstructorCall : IrDelegatingConstructorCallMessageType?): IrOperationMessageType
    abstract fun createIrOperation_doWhile(doWhile : IrDoWhileMessageType?): IrOperationMessageType
    abstract fun createIrOperation_enumConstructorCall(enumConstructorCall : IrEnumConstructorCallMessageType?): IrOperationMessageType
    abstract fun createIrOperation_functionReference(functionReference : IrFunctionReferenceMessageType?): IrOperationMessageType
    abstract fun createIrOperation_getClass(getClass : IrGetClassMessageType?): IrOperationMessageType
    abstract fun createIrOperation_getEnumValue(getEnumValue : IrGetEnumValueMessageType?): IrOperationMessageType
    abstract fun createIrOperation_getField(getField : IrGetFieldMessageType?): IrOperationMessageType
    abstract fun createIrOperation_getObject(getObject : IrGetObjectMessageType?): IrOperationMessageType
    abstract fun createIrOperation_getValue(getValue : IrGetValueMessageType?): IrOperationMessageType
    abstract fun createIrOperation_instanceInitializerCall(instanceInitializerCall : IrInstanceInitializerCallMessageType?): IrOperationMessageType
    abstract fun createIrOperation_propertyReference(propertyReference : IrPropertyReferenceMessageType?): IrOperationMessageType
    abstract fun createIrOperation_return_(return_ : IrReturnMessageType?): IrOperationMessageType
    abstract fun createIrOperation_setField(setField : IrSetFieldMessageType?): IrOperationMessageType
    abstract fun createIrOperation_setVariable(setVariable : IrSetVariableMessageType?): IrOperationMessageType
    abstract fun createIrOperation_stringConcat(stringConcat : IrStringConcatMessageType?): IrOperationMessageType
    abstract fun createIrOperation_throw_(throw_ : IrThrowMessageType?): IrOperationMessageType
    abstract fun createIrOperation_try_(try_ : IrTryMessageType?): IrOperationMessageType
    abstract fun createIrOperation_typeOp(typeOp : IrTypeOpMessageType?): IrOperationMessageType
    abstract fun createIrOperation_vararg(vararg : IrVarargMessageType?): IrOperationMessageType
    abstract fun createIrOperation_when_(when_ : IrWhenMessageType?): IrOperationMessageType
    abstract fun createIrOperation_while_(while_ : IrWhileMessageType?): IrOperationMessageType
    abstract fun createIrOperation_dynamicMember(dynamicMember : IrDynamicMemberExpressionMessageType?): IrOperationMessageType
    abstract fun createIrOperation_dynamicOperator(dynamicOperator : IrDynamicOperatorExpressionMessageType?): IrOperationMessageType
    abstract fun createIrOperation_localDelegatedPropertyReference(localDelegatedPropertyReference : IrLocalDelegatedPropertyReferenceMessageType?): IrOperationMessageType
    abstract fun createIrOperation_constructorCall(constructorCall : IrConstructorCallMessageType?): IrOperationMessageType
    abstract fun createIrOperation_functionExpression(functionExpression : IrFunctionExpressionMessageType?): IrOperationMessageType

    abstract fun createIrTypeOperator(index: Int): IrTypeOperatorMessageType

    abstract fun createIrExpression(operation : IrOperationMessageType, type : Int, coordinates : CoordinatesMessageType): IrExpressionMessageType

    abstract fun createNullableIrExpression(expression : IrExpressionMessageType?): NullableIrExpressionMessageType

    abstract fun createIrDeclarationBase(symbol : Int, origin : IrDeclarationOriginMessageType, coordinates : CoordinatesMessageType, annotations : List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>): IrDeclarationBaseMessageType

    abstract fun createIrFunctionBase(base : IrDeclarationBaseMessageType, name : Int, visibility : VisibilityMessageType, isInline : Boolean, isExternal : Boolean, typeParameters : List<org.jetbrains.kotlin.ir.declarations.IrTypeParameter>, dispatchReceiver : IrValueParameterMessageType?, extensionReceiver : IrValueParameterMessageType?, valueParameter : List<IrValueParameterMessageType>, body : Int?, returnType : Int): IrFunctionBaseMessageType

    abstract fun createIrFunction(base : IrFunctionBaseMessageType, modality : ModalityKindMessageType, isTailrec : Boolean, isSuspend : Boolean, overridden : List<Int>): IrFunctionMessageType

    abstract fun createIrConstructor(base : IrFunctionBaseMessageType, isPrimary : Boolean): IrConstructorMessageType

    abstract fun createIrField(base : IrDeclarationBaseMessageType, initializer : Int?, name : Int, visibility : VisibilityMessageType, isFinal : Boolean, isExternal : Boolean, isStatic : Boolean, type : Int): IrFieldMessageType

    abstract fun createIrLocalDelegatedProperty(base : IrDeclarationBaseMessageType, name : Int, type : Int, isVar : Boolean, delegate : IrVariableMessageType, getter : IrFunctionMessageType?, setter : IrFunctionMessageType?): IrLocalDelegatedPropertyMessageType

    abstract fun createIrProperty(base : IrDeclarationBaseMessageType, name : Int, visibility : VisibilityMessageType, modality : ModalityKindMessageType, isVar : Boolean, isConst : Boolean, isLateinit : Boolean, isDelegated : Boolean, isExternal : Boolean, backingField : IrFieldMessageType?, getter : IrFunctionMessageType?, setter : IrFunctionMessageType?): IrPropertyMessageType

    abstract fun createIrVariable(base : IrDeclarationBaseMessageType, name : Int, type : Int, isVar : Boolean, isConst : Boolean, isLateinit : Boolean, initializer : IrExpressionMessageType?): IrVariableMessageType

    abstract fun createClassKind(index: Int): ClassKindMessageType

    abstract fun createModalityKind(index: Int): ModalityKindMessageType

    abstract fun createIrValueParameter(base : IrDeclarationBaseMessageType, name : Int, index : Int, type : Int, varargElementType : Int?, isCrossinline : Boolean, isNoinline : Boolean, defaultValue : Int?): IrValueParameterMessageType

    abstract fun createIrTypeParameter(base : IrDeclarationBaseMessageType, name : Int, index : Int, variance : IrTypeVarianceMessageType, superType : List<Int>, isReified : Boolean): IrTypeParameterMessageType

    abstract fun createIrTypeParameterContainer(typeParameter : List<IrTypeParameterMessageType>): List<org.jetbrains.kotlin.ir.declarations.IrTypeParameter>

    abstract fun createIrClass(base : IrDeclarationBaseMessageType, name : Int, kind : ClassKindMessageType, visibility : VisibilityMessageType, modality : ModalityKindMessageType, isCompanion : Boolean, isInner : Boolean, isData : Boolean, isExternal : Boolean, isInline : Boolean, thisReceiver : IrValueParameterMessageType?, typeParameters : List<org.jetbrains.kotlin.ir.declarations.IrTypeParameter>, declarationContainer : List<org.jetbrains.kotlin.ir.declarations.IrDeclaration>, superType : List<Int>): IrClassMessageType

    abstract fun createIrTypeAlias(base : IrDeclarationBaseMessageType, name : Int, visibility : VisibilityMessageType, typeParameters : List<org.jetbrains.kotlin.ir.declarations.IrTypeParameter>, expandedType : Int, isActual : Boolean): IrTypeAliasMessageType

    abstract fun createIrEnumEntry(base : IrDeclarationBaseMessageType, initializer : Int?, correspondingClass : IrClassMessageType?, name : Int): IrEnumEntryMessageType

    abstract fun createIrAnonymousInit(base : IrDeclarationBaseMessageType, body : Int): IrAnonymousInitMessageType

    abstract fun createIrDeclaration_irAnonymousInit(irAnonymousInit : IrAnonymousInitMessageType?): IrDeclarationMessageType
    abstract fun createIrDeclaration_irClass(irClass : IrClassMessageType?): IrDeclarationMessageType
    abstract fun createIrDeclaration_irConstructor(irConstructor : IrConstructorMessageType?): IrDeclarationMessageType
    abstract fun createIrDeclaration_irEnumEntry(irEnumEntry : IrEnumEntryMessageType?): IrDeclarationMessageType
    abstract fun createIrDeclaration_irField(irField : IrFieldMessageType?): IrDeclarationMessageType
    abstract fun createIrDeclaration_irFunction(irFunction : IrFunctionMessageType?): IrDeclarationMessageType
    abstract fun createIrDeclaration_irProperty(irProperty : IrPropertyMessageType?): IrDeclarationMessageType
    abstract fun createIrDeclaration_irTypeParameter(irTypeParameter : IrTypeParameterMessageType?): IrDeclarationMessageType
    abstract fun createIrDeclaration_irVariable(irVariable : IrVariableMessageType?): IrDeclarationMessageType
    abstract fun createIrDeclaration_irValueParameter(irValueParameter : IrValueParameterMessageType?): IrDeclarationMessageType
    abstract fun createIrDeclaration_irLocalDelegatedProperty(irLocalDelegatedProperty : IrLocalDelegatedPropertyMessageType?): IrDeclarationMessageType
    abstract fun createIrDeclaration_irTypeAlias(irTypeAlias : IrTypeAliasMessageType?): IrDeclarationMessageType

    abstract fun createIrBranch(condition : IrExpressionMessageType, result : IrExpressionMessageType): IrBranchMessageType

    abstract fun createIrBlockBody(statement : List<IrStatementMessageType>): IrBlockBodyMessageType

    abstract fun createIrCatch(catchParameter : IrVariableMessageType, result : IrExpressionMessageType): IrCatchMessageType

    abstract fun createIrSyntheticBodyKind(index: Int): IrSyntheticBodyKindMessageType

    abstract fun createIrSyntheticBody(kind : IrSyntheticBodyKindMessageType): IrSyntheticBodyMessageType

    abstract fun createIrStatement_declaration(coordinates : CoordinatesMessageType, declaration : IrDeclarationMessageType?): IrStatementMessageType
    abstract fun createIrStatement_expression(coordinates : CoordinatesMessageType, expression : IrExpressionMessageType?): IrStatementMessageType
    abstract fun createIrStatement_blockBody(coordinates : CoordinatesMessageType, blockBody : IrBlockBodyMessageType?): IrStatementMessageType
    abstract fun createIrStatement_branch(coordinates : CoordinatesMessageType, branch : IrBranchMessageType?): IrStatementMessageType
    abstract fun createIrStatement_catch(coordinates : CoordinatesMessageType, catch : IrCatchMessageType?): IrStatementMessageType
    abstract fun createIrStatement_syntheticBody(coordinates : CoordinatesMessageType, syntheticBody : IrSyntheticBodyMessageType?): IrStatementMessageType

    open fun readDescriptorReference(): DescriptorReferenceMessageType {
        var package_fq_name__: FqNameMessageType? = null
        var class_fq_name__: FqNameMessageType? = null
        var name__: Int? = null
        var uniq_id__: UniqIdMessageType? = null
        var is_getter__: Boolean = false
        var is_setter__: Boolean = false
        var is_backing_field__: Boolean = false
        var is_fake_override__: Boolean = false
        var is_default_constructor__: Boolean = false
        var is_enum_entry__: Boolean = false
        var is_enum_special__: Boolean = false
        var is_type_parameter__: Boolean = false
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> package_fq_name__ = readWithLength { readFqName() }
                    2 -> class_fq_name__ = readWithLength { readFqName() }
                    3 -> name__ = readWithLength { readIrDataIndex() }
                    4 -> uniq_id__ = readWithLength { readUniqId() }
                    5 -> is_getter__ = readBool()
                    6 -> is_setter__ = readBool()
                    7 -> is_backing_field__ = readBool()
                    8 -> is_fake_override__ = readBool()
                    9 -> is_default_constructor__ = readBool()
                    10 -> is_enum_entry__ = readBool()
                    11 -> is_enum_special__ = readBool()
                    12 -> is_type_parameter__ = readBool()
                    else -> skip(type)
                }
            }
        }
        return createDescriptorReference(package_fq_name__!!, class_fq_name__!!, name__!!, uniq_id__, is_getter__, is_setter__, is_backing_field__, is_fake_override__, is_default_constructor__, is_enum_entry__, is_enum_special__, is_type_parameter__)
    }

    open fun readUniqId(): UniqIdMessageType {
        var index__: Long = 0L
        var isLocal__: Boolean = false
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> index__ = readInt64()
                    2 -> isLocal__ = readBool()
                    else -> skip(type)
                }
            }
        }
        return createUniqId(index__, isLocal__)
    }

    open fun readCoordinates(): CoordinatesMessageType {
        var start_offset__: Int = 0
        var end_offset__: Int = 0
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> start_offset__ = readInt32()
                    2 -> end_offset__ = readInt32()
                    else -> skip(type)
                }
            }
        }
        return createCoordinates(start_offset__, end_offset__)
    }

    open fun readVisibility(): VisibilityMessageType {
        var name__: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> name__ = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createVisibility(name__!!)
    }

    open fun readIrStatementOrigin(): IrStatementOriginMessageType {
        var name__: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> name__ = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrStatementOrigin(name__!!)
    }

    open fun readIrDeclarationOrigin(): IrDeclarationOriginMessageType {
        var origin__: KnownOriginMessageType? = null
        var custom__: Int? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> {
                        origin__ = createKnownOrigin(readInt32())
                        oneOfIndex = 1
                    }
                    2 -> {
                        custom__ = readWithLength { readIrDataIndex() }
                        oneOfIndex = 2
                    }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            1 -> return createIrDeclarationOrigin_origin(origin__!!)
            2 -> return createIrDeclarationOrigin_custom(custom__!!)
            else -> error("Incorrect oneOf index: " + oneOfIndex)
        }
    }

    open fun readIrDataIndex(): Int {
        var index__: Int = 0
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> index__ = readInt32()
                    else -> skip(type)
                }
            }
        }
        return createIrDataIndex(index__)
    }

    open fun readFqName(): FqNameMessageType {
        var segment__: MutableList<Int> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> segment__.add(readWithLength { readIrDataIndex() })
                    else -> skip(type)
                }
            }
        }
        return createFqName(segment__)
    }

    open fun readIrDeclarationContainer(): List<org.jetbrains.kotlin.ir.declarations.IrDeclaration> {
        var declaration__: MutableList<IrDeclarationMessageType> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> declaration__.add(readWithLength { readIrDeclaration() })
                    else -> skip(type)
                }
            }
        }
        return createIrDeclarationContainer(declaration__)
    }

    open fun readFileEntry(): FileEntryMessageType {
        var name__: String = ""
        var line_start_offsets__: MutableList<Int> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> name__ = readString()
                    2 -> line_start_offsets__.add(readInt32())
                    else -> skip(type)
                }
            }
        }
        return createFileEntry(name__, line_start_offsets__)
    }

    open fun readIrFile(): IrFileMessageType {
        var declaration_id__: MutableList<UniqIdMessageType> = mutableListOf()
        var file_entry__: FileEntryMessageType? = null
        var fq_name__: FqNameMessageType? = null
        var annotations__: List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>? = null
        var explicitly_exported_to_compiler__: MutableList<Int> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> declaration_id__.add(readWithLength { readUniqId() })
                    2 -> file_entry__ = readWithLength { readFileEntry() }
                    3 -> fq_name__ = readWithLength { readFqName() }
                    4 -> annotations__ = readWithLength { readAnnotations() }
                    5 -> explicitly_exported_to_compiler__.add(readWithLength { readIrDataIndex() })
                    else -> skip(type)
                }
            }
        }
        return createIrFile(declaration_id__, file_entry__!!, fq_name__!!, annotations__!!, explicitly_exported_to_compiler__)
    }

    open fun readStringTable(): Array<String> {
        var strings__: MutableList<String> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> strings__.add(readString())
                    else -> skip(type)
                }
            }
        }
        return createStringTable(strings__)
    }

    open fun readIrSymbolData(): IrSymbolDataMessageType {
        var kind__: Int? = null
        var uniq_id__: UniqIdMessageType? = null
        var top_level_uniq_id__: UniqIdMessageType? = null
        var fqname__: FqNameMessageType? = null
        var descriptor_reference__: DescriptorReferenceMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> kind__ = createIrSymbolKind(readInt32())
                    2 -> uniq_id__ = readWithLength { readUniqId() }
                    3 -> top_level_uniq_id__ = readWithLength { readUniqId() }
                    4 -> fqname__ = readWithLength { readFqName() }
                    5 -> descriptor_reference__ = readWithLength { readDescriptorReference() }
                    else -> skip(type)
                }
            }
        }
        return createIrSymbolData(kind__!!, uniq_id__!!, top_level_uniq_id__!!, fqname__, descriptor_reference__)
    }

    open fun readIrSymbolTable(): Array<org.jetbrains.kotlin.ir.symbols.IrSymbol> {
        var symbols__: MutableList<IrSymbolDataMessageType> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbols__.add(readWithLength { readIrSymbolData() })
                    else -> skip(type)
                }
            }
        }
        return createIrSymbolTable(symbols__)
    }

    open fun readAnnotations(): List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall> {
        var annotation__: MutableList<IrConstructorCallMessageType> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> annotation__.add(readWithLength { readIrConstructorCall() })
                    else -> skip(type)
                }
            }
        }
        return createAnnotations(annotation__)
    }

    open fun readTypeArguments(): List<org.jetbrains.kotlin.ir.types.IrType> {
        var type_argument__: MutableList<Int> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> type_argument__.add(readWithLength { readIrDataIndex() })
                    else -> skip(type)
                }
            }
        }
        return createTypeArguments(type_argument__)
    }

    open fun readIrStarProjection(): IrStarProjectionMessageType {
        var void__: Boolean? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> void__ = readBool()
                    else -> skip(type)
                }
            }
        }
        return createIrStarProjection(void__)
    }

    open fun readIrTypeProjection(): IrTypeProjectionMessageType {
        var variance__: IrTypeVarianceMessageType? = null
        var type__: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> variance__ = createIrTypeVariance(readInt32())
                    2 -> type__ = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrTypeProjection(variance__!!, type__!!)
    }

    open fun readIrTypeArgument(): IrTypeArgumentMessageType {
        var star__: IrStarProjectionMessageType? = null
        var type__: IrTypeProjectionMessageType? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> {
                        star__ = readWithLength { readIrStarProjection() }
                        oneOfIndex = 1
                    }
                    2 -> {
                        type__ = readWithLength { readIrTypeProjection() }
                        oneOfIndex = 2
                    }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            1 -> return createIrTypeArgument_star(star__!!)
            2 -> return createIrTypeArgument_type(type__!!)
            else -> error("Incorrect oneOf index: " + oneOfIndex)
        }
    }

    open fun readIrSimpleType(): IrSimpleTypeMessageType {
        var annotations__: List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>? = null
        var classifier__: Int? = null
        var has_question_mark__: Boolean = false
        var argument__: MutableList<IrTypeArgumentMessageType> = mutableListOf()
        var abbreviation__: IrTypeAbbreviationMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> annotations__ = readWithLength { readAnnotations() }
                    2 -> classifier__ = readWithLength { readIrDataIndex() }
                    3 -> has_question_mark__ = readBool()
                    4 -> argument__.add(readWithLength { readIrTypeArgument() })
                    5 -> abbreviation__ = readWithLength { readIrTypeAbbreviation() }
                    else -> skip(type)
                }
            }
        }
        return createIrSimpleType(annotations__!!, classifier__!!, has_question_mark__, argument__, abbreviation__)
    }

    open fun readIrTypeAbbreviation(): IrTypeAbbreviationMessageType {
        var annotations__: List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>? = null
        var type_alias__: Int? = null
        var has_question_mark__: Boolean = false
        var argument__: MutableList<IrTypeArgumentMessageType> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> annotations__ = readWithLength { readAnnotations() }
                    2 -> type_alias__ = readWithLength { readIrDataIndex() }
                    3 -> has_question_mark__ = readBool()
                    4 -> argument__.add(readWithLength { readIrTypeArgument() })
                    else -> skip(type)
                }
            }
        }
        return createIrTypeAbbreviation(annotations__!!, type_alias__!!, has_question_mark__, argument__)
    }

    open fun readIrDynamicType(): IrDynamicTypeMessageType {
        var annotations__: List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> annotations__ = readWithLength { readAnnotations() }
                    else -> skip(type)
                }
            }
        }
        return createIrDynamicType(annotations__!!)
    }

    open fun readIrErrorType(): IrErrorTypeMessageType {
        var annotations__: List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> annotations__ = readWithLength { readAnnotations() }
                    else -> skip(type)
                }
            }
        }
        return createIrErrorType(annotations__!!)
    }

    open fun readIrType(): IrTypeMessageType {
        var simple__: IrSimpleTypeMessageType? = null
        var dynamic__: IrDynamicTypeMessageType? = null
        var error__: IrErrorTypeMessageType? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> {
                        simple__ = readWithLength { readIrSimpleType() }
                        oneOfIndex = 1
                    }
                    2 -> {
                        dynamic__ = readWithLength { readIrDynamicType() }
                        oneOfIndex = 2
                    }
                    3 -> {
                        error__ = readWithLength { readIrErrorType() }
                        oneOfIndex = 3
                    }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            1 -> return createIrType_simple(simple__!!)
            2 -> return createIrType_dynamic(dynamic__!!)
            3 -> return createIrType_error(error__!!)
            else -> error("Incorrect oneOf index: " + oneOfIndex)
        }
    }

    open fun readIrTypeTable(): Array<org.jetbrains.kotlin.ir.types.IrType> {
        var types__: MutableList<IrTypeMessageType> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> types__.add(readWithLength { readIrType() })
                    else -> skip(type)
                }
            }
        }
        return createIrTypeTable(types__)
    }

    open fun readIrBreak(): IrBreakMessageType {
        var loop_id__: Int = 0
        var label__: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> loop_id__ = readInt32()
                    2 -> label__ = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrBreak(loop_id__, label__)
    }

    open fun readIrBlock(): IrBlockMessageType {
        var origin__: IrStatementOriginMessageType? = null
        var statement__: MutableList<IrStatementMessageType> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> origin__ = readWithLength { readIrStatementOrigin() }
                    2 -> statement__.add(readWithLength { readIrStatement() })
                    else -> skip(type)
                }
            }
        }
        return createIrBlock(origin__, statement__)
    }

    open fun readMemberAccessCommon(): MemberAccessCommonMessageType {
        var dispatch_receiver__: IrExpressionMessageType? = null
        var extension_receiver__: IrExpressionMessageType? = null
        var value_argument__: MutableList<NullableIrExpressionMessageType> = mutableListOf()
        var type_arguments__: List<org.jetbrains.kotlin.ir.types.IrType>? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> dispatch_receiver__ = readWithLength { readIrExpression() }
                    2 -> extension_receiver__ = readWithLength { readIrExpression() }
                    3 -> value_argument__.add(readWithLength { readNullableIrExpression() })
                    4 -> type_arguments__ = readWithLength { readTypeArguments() }
                    else -> skip(type)
                }
            }
        }
        return createMemberAccessCommon(dispatch_receiver__, extension_receiver__, value_argument__, type_arguments__!!)
    }

    open fun readIrCall(): IrCallMessageType {
        var symbol__: Int? = null
        var member_access__: MemberAccessCommonMessageType? = null
        var super__: Int? = null
        var origin__: IrStatementOriginMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol__ = readWithLength { readIrDataIndex() }
                    2 -> member_access__ = readWithLength { readMemberAccessCommon() }
                    3 -> super__ = readWithLength { readIrDataIndex() }
                    4 -> origin__ = readWithLength { readIrStatementOrigin() }
                    else -> skip(type)
                }
            }
        }
        return createIrCall(symbol__!!, member_access__!!, super__, origin__)
    }

    open fun readIrConstructorCall(): IrConstructorCallMessageType {
        var symbol__: Int? = null
        var constructor_type_arguments_count__: Int = 0
        var member_access__: MemberAccessCommonMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol__ = readWithLength { readIrDataIndex() }
                    2 -> constructor_type_arguments_count__ = readInt32()
                    3 -> member_access__ = readWithLength { readMemberAccessCommon() }
                    else -> skip(type)
                }
            }
        }
        return createIrConstructorCall(symbol__!!, constructor_type_arguments_count__, member_access__!!)
    }

    open fun readIrFunctionReference(): IrFunctionReferenceMessageType {
        var symbol__: Int? = null
        var origin__: IrStatementOriginMessageType? = null
        var member_access__: MemberAccessCommonMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol__ = readWithLength { readIrDataIndex() }
                    2 -> origin__ = readWithLength { readIrStatementOrigin() }
                    3 -> member_access__ = readWithLength { readMemberAccessCommon() }
                    else -> skip(type)
                }
            }
        }
        return createIrFunctionReference(symbol__!!, origin__, member_access__!!)
    }

    open fun readIrLocalDelegatedPropertyReference(): IrLocalDelegatedPropertyReferenceMessageType {
        var delegate__: Int? = null
        var getter__: Int? = null
        var setter__: Int? = null
        var symbol__: Int? = null
        var origin__: IrStatementOriginMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> delegate__ = readWithLength { readIrDataIndex() }
                    2 -> getter__ = readWithLength { readIrDataIndex() }
                    3 -> setter__ = readWithLength { readIrDataIndex() }
                    4 -> symbol__ = readWithLength { readIrDataIndex() }
                    5 -> origin__ = readWithLength { readIrStatementOrigin() }
                    else -> skip(type)
                }
            }
        }
        return createIrLocalDelegatedPropertyReference(delegate__!!, getter__, setter__, symbol__!!, origin__)
    }

    open fun readIrPropertyReference(): IrPropertyReferenceMessageType {
        var field__: Int? = null
        var getter__: Int? = null
        var setter__: Int? = null
        var origin__: IrStatementOriginMessageType? = null
        var member_access__: MemberAccessCommonMessageType? = null
        var symbol__: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> field__ = readWithLength { readIrDataIndex() }
                    2 -> getter__ = readWithLength { readIrDataIndex() }
                    3 -> setter__ = readWithLength { readIrDataIndex() }
                    4 -> origin__ = readWithLength { readIrStatementOrigin() }
                    5 -> member_access__ = readWithLength { readMemberAccessCommon() }
                    6 -> symbol__ = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrPropertyReference(field__, getter__, setter__, origin__, member_access__!!, symbol__!!)
    }

    open fun readIrComposite(): IrCompositeMessageType {
        var statement__: MutableList<IrStatementMessageType> = mutableListOf()
        var origin__: IrStatementOriginMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> statement__.add(readWithLength { readIrStatement() })
                    2 -> origin__ = readWithLength { readIrStatementOrigin() }
                    else -> skip(type)
                }
            }
        }
        return createIrComposite(statement__, origin__)
    }

    open fun readIrClassReference(): IrClassReferenceMessageType {
        var class_symbol__: Int? = null
        var class_type__: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> class_symbol__ = readWithLength { readIrDataIndex() }
                    2 -> class_type__ = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrClassReference(class_symbol__!!, class_type__!!)
    }

    open fun readIrConst(): IrConstMessageType<*> {
        var null__: Boolean? = null
        var boolean__: Boolean? = null
        var char__: Int? = null
        var byte__: Int? = null
        var short__: Int? = null
        var int__: Int? = null
        var long__: Long? = null
        var float__: Float? = null
        var double__: Double? = null
        var string__: Int? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> {
                        null__ = readBool()
                        oneOfIndex = 1
                    }
                    2 -> {
                        boolean__ = readBool()
                        oneOfIndex = 2
                    }
                    3 -> {
                        char__ = readInt32()
                        oneOfIndex = 3
                    }
                    4 -> {
                        byte__ = readInt32()
                        oneOfIndex = 4
                    }
                    5 -> {
                        short__ = readInt32()
                        oneOfIndex = 5
                    }
                    6 -> {
                        int__ = readInt32()
                        oneOfIndex = 6
                    }
                    7 -> {
                        long__ = readInt64()
                        oneOfIndex = 7
                    }
                    8 -> {
                        float__ = readFloat()
                        oneOfIndex = 8
                    }
                    9 -> {
                        double__ = readDouble()
                        oneOfIndex = 9
                    }
                    10 -> {
                        string__ = readWithLength { readIrDataIndex() }
                        oneOfIndex = 10
                    }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            1 -> return createIrConst_null_(null__!!)
            2 -> return createIrConst_boolean(boolean__!!)
            3 -> return createIrConst_char(char__!!)
            4 -> return createIrConst_byte(byte__!!)
            5 -> return createIrConst_short(short__!!)
            6 -> return createIrConst_int(int__!!)
            7 -> return createIrConst_long(long__!!)
            8 -> return createIrConst_float(float__!!)
            9 -> return createIrConst_double(double__!!)
            10 -> return createIrConst_string(string__!!)
            else -> error("Incorrect oneOf index: " + oneOfIndex)
        }
    }

    open fun readIrContinue(): IrContinueMessageType {
        var loop_id__: Int = 0
        var label__: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> loop_id__ = readInt32()
                    2 -> label__ = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrContinue(loop_id__, label__)
    }

    open fun readIrDelegatingConstructorCall(): IrDelegatingConstructorCallMessageType {
        var symbol__: Int? = null
        var member_access__: MemberAccessCommonMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol__ = readWithLength { readIrDataIndex() }
                    2 -> member_access__ = readWithLength { readMemberAccessCommon() }
                    else -> skip(type)
                }
            }
        }
        return createIrDelegatingConstructorCall(symbol__!!, member_access__!!)
    }

    open fun readIrDoWhile(): IrDoWhileMessageType {
        var loop__: LoopMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> loop__ = readWithLength { readLoop() }
                    else -> skip(type)
                }
            }
        }
        return createIrDoWhile(loop__!!)
    }

    open fun readIrEnumConstructorCall(): IrEnumConstructorCallMessageType {
        var symbol__: Int? = null
        var member_access__: MemberAccessCommonMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol__ = readWithLength { readIrDataIndex() }
                    2 -> member_access__ = readWithLength { readMemberAccessCommon() }
                    else -> skip(type)
                }
            }
        }
        return createIrEnumConstructorCall(symbol__!!, member_access__!!)
    }

    open fun readIrGetClass(): IrGetClassMessageType {
        var argument__: IrExpressionMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> argument__ = readWithLength { readIrExpression() }
                    else -> skip(type)
                }
            }
        }
        return createIrGetClass(argument__!!)
    }

    open fun readIrGetEnumValue(): IrGetEnumValueMessageType {
        var symbol__: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    2 -> symbol__ = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrGetEnumValue(symbol__!!)
    }

    open fun readFieldAccessCommon(): FieldAccessCommonMessageType {
        var symbol__: Int? = null
        var super__: Int? = null
        var receiver__: IrExpressionMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol__ = readWithLength { readIrDataIndex() }
                    2 -> super__ = readWithLength { readIrDataIndex() }
                    3 -> receiver__ = readWithLength { readIrExpression() }
                    else -> skip(type)
                }
            }
        }
        return createFieldAccessCommon(symbol__!!, super__, receiver__)
    }

    open fun readIrGetField(): IrGetFieldMessageType {
        var field_access__: FieldAccessCommonMessageType? = null
        var origin__: IrStatementOriginMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> field_access__ = readWithLength { readFieldAccessCommon() }
                    2 -> origin__ = readWithLength { readIrStatementOrigin() }
                    else -> skip(type)
                }
            }
        }
        return createIrGetField(field_access__!!, origin__)
    }

    open fun readIrGetValue(): IrGetValueMessageType {
        var symbol__: Int? = null
        var origin__: IrStatementOriginMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol__ = readWithLength { readIrDataIndex() }
                    2 -> origin__ = readWithLength { readIrStatementOrigin() }
                    else -> skip(type)
                }
            }
        }
        return createIrGetValue(symbol__!!, origin__)
    }

    open fun readIrGetObject(): IrGetObjectMessageType {
        var symbol__: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol__ = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrGetObject(symbol__!!)
    }

    open fun readIrInstanceInitializerCall(): IrInstanceInitializerCallMessageType {
        var symbol__: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol__ = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrInstanceInitializerCall(symbol__!!)
    }

    open fun readLoop(): LoopMessageType {
        var loop_id__: Int = 0
        var condition__: IrExpressionMessageType? = null
        var label__: Int? = null
        var body__: IrExpressionMessageType? = null
        var origin__: IrStatementOriginMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> loop_id__ = readInt32()
                    2 -> condition__ = readWithLength { readIrExpression() }
                    3 -> label__ = readWithLength { readIrDataIndex() }
                    4 -> body__ = readWithLength { readIrExpression() }
                    5 -> origin__ = readWithLength { readIrStatementOrigin() }
                    else -> skip(type)
                }
            }
        }
        return createLoop(loop_id__, condition__!!, label__, body__, origin__)
    }

    open fun readIrReturn(): IrReturnMessageType {
        var return_target__: Int? = null
        var value__: IrExpressionMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> return_target__ = readWithLength { readIrDataIndex() }
                    2 -> value__ = readWithLength { readIrExpression() }
                    else -> skip(type)
                }
            }
        }
        return createIrReturn(return_target__!!, value__!!)
    }

    open fun readIrSetField(): IrSetFieldMessageType {
        var field_access__: FieldAccessCommonMessageType? = null
        var value__: IrExpressionMessageType? = null
        var origin__: IrStatementOriginMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> field_access__ = readWithLength { readFieldAccessCommon() }
                    2 -> value__ = readWithLength { readIrExpression() }
                    3 -> origin__ = readWithLength { readIrStatementOrigin() }
                    else -> skip(type)
                }
            }
        }
        return createIrSetField(field_access__!!, value__!!, origin__)
    }

    open fun readIrSetVariable(): IrSetVariableMessageType {
        var symbol__: Int? = null
        var value__: IrExpressionMessageType? = null
        var origin__: IrStatementOriginMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol__ = readWithLength { readIrDataIndex() }
                    2 -> value__ = readWithLength { readIrExpression() }
                    3 -> origin__ = readWithLength { readIrStatementOrigin() }
                    else -> skip(type)
                }
            }
        }
        return createIrSetVariable(symbol__!!, value__!!, origin__)
    }

    open fun readIrSpreadElement(): IrSpreadElementMessageType {
        var expression__: IrExpressionMessageType? = null
        var coordinates__: CoordinatesMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> expression__ = readWithLength { readIrExpression() }
                    2 -> coordinates__ = readWithLength { readCoordinates() }
                    else -> skip(type)
                }
            }
        }
        return createIrSpreadElement(expression__!!, coordinates__!!)
    }

    open fun readIrStringConcat(): IrStringConcatMessageType {
        var argument__: MutableList<IrExpressionMessageType> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> argument__.add(readWithLength { readIrExpression() })
                    else -> skip(type)
                }
            }
        }
        return createIrStringConcat(argument__)
    }

    open fun readIrThrow(): IrThrowMessageType {
        var value__: IrExpressionMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> value__ = readWithLength { readIrExpression() }
                    else -> skip(type)
                }
            }
        }
        return createIrThrow(value__!!)
    }

    open fun readIrTry(): IrTryMessageType {
        var result__: IrExpressionMessageType? = null
        var catch__: MutableList<IrStatementMessageType> = mutableListOf()
        var finally__: IrExpressionMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> result__ = readWithLength { readIrExpression() }
                    2 -> catch__.add(readWithLength { readIrStatement() })
                    3 -> finally__ = readWithLength { readIrExpression() }
                    else -> skip(type)
                }
            }
        }
        return createIrTry(result__!!, catch__, finally__)
    }

    open fun readIrTypeOp(): IrTypeOpMessageType {
        var operator__: IrTypeOperatorMessageType? = null
        var operand__: Int? = null
        var argument__: IrExpressionMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> operator__ = createIrTypeOperator(readInt32())
                    2 -> operand__ = readWithLength { readIrDataIndex() }
                    3 -> argument__ = readWithLength { readIrExpression() }
                    else -> skip(type)
                }
            }
        }
        return createIrTypeOp(operator__!!, operand__!!, argument__!!)
    }

    open fun readIrVararg(): IrVarargMessageType {
        var element_type__: Int? = null
        var element__: MutableList<IrVarargElementMessageType> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> element_type__ = readWithLength { readIrDataIndex() }
                    2 -> element__.add(readWithLength { readIrVarargElement() })
                    else -> skip(type)
                }
            }
        }
        return createIrVararg(element_type__!!, element__)
    }

    open fun readIrVarargElement(): IrVarargElementMessageType {
        var expression__: IrExpressionMessageType? = null
        var spread_element__: IrSpreadElementMessageType? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> {
                        expression__ = readWithLength { readIrExpression() }
                        oneOfIndex = 1
                    }
                    2 -> {
                        spread_element__ = readWithLength { readIrSpreadElement() }
                        oneOfIndex = 2
                    }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            1 -> return createIrVarargElement_expression(expression__!!)
            2 -> return createIrVarargElement_spreadElement(spread_element__!!)
            else -> error("Incorrect oneOf index: " + oneOfIndex)
        }
    }

    open fun readIrWhen(): IrWhenMessageType {
        var branch__: MutableList<IrStatementMessageType> = mutableListOf()
        var origin__: IrStatementOriginMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> branch__.add(readWithLength { readIrStatement() })
                    2 -> origin__ = readWithLength { readIrStatementOrigin() }
                    else -> skip(type)
                }
            }
        }
        return createIrWhen(branch__, origin__)
    }

    open fun readIrWhile(): IrWhileMessageType {
        var loop__: LoopMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> loop__ = readWithLength { readLoop() }
                    else -> skip(type)
                }
            }
        }
        return createIrWhile(loop__!!)
    }

    open fun readIrFunctionExpression(): IrFunctionExpressionMessageType {
        var function__: IrFunctionMessageType? = null
        var origin__: IrStatementOriginMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> function__ = readWithLength { readIrFunction() }
                    2 -> origin__ = readWithLength { readIrStatementOrigin() }
                    else -> skip(type)
                }
            }
        }
        return createIrFunctionExpression(function__!!, origin__!!)
    }

    open fun readIrDynamicMemberExpression(): IrDynamicMemberExpressionMessageType {
        var memberName__: Int? = null
        var receiver__: IrExpressionMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> memberName__ = readWithLength { readIrDataIndex() }
                    2 -> receiver__ = readWithLength { readIrExpression() }
                    else -> skip(type)
                }
            }
        }
        return createIrDynamicMemberExpression(memberName__!!, receiver__!!)
    }

    open fun readIrDynamicOperatorExpression(): IrDynamicOperatorExpressionMessageType {
        var operator__: IrDynamicOperatorMessageType? = null
        var receiver__: IrExpressionMessageType? = null
        var argument__: MutableList<IrExpressionMessageType> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> operator__ = createIrDynamicOperator(readInt32())
                    2 -> receiver__ = readWithLength { readIrExpression() }
                    3 -> argument__.add(readWithLength { readIrExpression() })
                    else -> skip(type)
                }
            }
        }
        return createIrDynamicOperatorExpression(operator__!!, receiver__!!, argument__)
    }

    open fun readIrOperation(): IrOperationMessageType {
        var block__: IrBlockMessageType? = null
        var break__: IrBreakMessageType? = null
        var call__: IrCallMessageType? = null
        var class_reference__: IrClassReferenceMessageType? = null
        var composite__: IrCompositeMessageType? = null
        var const__: IrConstMessageType<*>? = null
        var continue__: IrContinueMessageType? = null
        var delegating_constructor_call__: IrDelegatingConstructorCallMessageType? = null
        var do_while__: IrDoWhileMessageType? = null
        var enum_constructor_call__: IrEnumConstructorCallMessageType? = null
        var function_reference__: IrFunctionReferenceMessageType? = null
        var get_class__: IrGetClassMessageType? = null
        var get_enum_value__: IrGetEnumValueMessageType? = null
        var get_field__: IrGetFieldMessageType? = null
        var get_object__: IrGetObjectMessageType? = null
        var get_value__: IrGetValueMessageType? = null
        var instance_initializer_call__: IrInstanceInitializerCallMessageType? = null
        var property_reference__: IrPropertyReferenceMessageType? = null
        var return__: IrReturnMessageType? = null
        var set_field__: IrSetFieldMessageType? = null
        var set_variable__: IrSetVariableMessageType? = null
        var string_concat__: IrStringConcatMessageType? = null
        var throw__: IrThrowMessageType? = null
        var try__: IrTryMessageType? = null
        var type_op__: IrTypeOpMessageType? = null
        var vararg__: IrVarargMessageType? = null
        var when__: IrWhenMessageType? = null
        var while__: IrWhileMessageType? = null
        var dynamic_member__: IrDynamicMemberExpressionMessageType? = null
        var dynamic_operator__: IrDynamicOperatorExpressionMessageType? = null
        var local_delegated_property_reference__: IrLocalDelegatedPropertyReferenceMessageType? = null
        var constructor_call__: IrConstructorCallMessageType? = null
        var function_expression__: IrFunctionExpressionMessageType? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> {
                        block__ = readWithLength { readIrBlock() }
                        oneOfIndex = 1
                    }
                    2 -> {
                        break__ = readWithLength { readIrBreak() }
                        oneOfIndex = 2
                    }
                    3 -> {
                        call__ = readWithLength { readIrCall() }
                        oneOfIndex = 3
                    }
                    4 -> {
                        class_reference__ = readWithLength { readIrClassReference() }
                        oneOfIndex = 4
                    }
                    5 -> {
                        composite__ = readWithLength { readIrComposite() }
                        oneOfIndex = 5
                    }
                    6 -> {
                        const__ = readWithLength { readIrConst() }
                        oneOfIndex = 6
                    }
                    7 -> {
                        continue__ = readWithLength { readIrContinue() }
                        oneOfIndex = 7
                    }
                    8 -> {
                        delegating_constructor_call__ = readWithLength { readIrDelegatingConstructorCall() }
                        oneOfIndex = 8
                    }
                    9 -> {
                        do_while__ = readWithLength { readIrDoWhile() }
                        oneOfIndex = 9
                    }
                    10 -> {
                        enum_constructor_call__ = readWithLength { readIrEnumConstructorCall() }
                        oneOfIndex = 10
                    }
                    11 -> {
                        function_reference__ = readWithLength { readIrFunctionReference() }
                        oneOfIndex = 11
                    }
                    12 -> {
                        get_class__ = readWithLength { readIrGetClass() }
                        oneOfIndex = 12
                    }
                    13 -> {
                        get_enum_value__ = readWithLength { readIrGetEnumValue() }
                        oneOfIndex = 13
                    }
                    14 -> {
                        get_field__ = readWithLength { readIrGetField() }
                        oneOfIndex = 14
                    }
                    15 -> {
                        get_object__ = readWithLength { readIrGetObject() }
                        oneOfIndex = 15
                    }
                    16 -> {
                        get_value__ = readWithLength { readIrGetValue() }
                        oneOfIndex = 16
                    }
                    17 -> {
                        instance_initializer_call__ = readWithLength { readIrInstanceInitializerCall() }
                        oneOfIndex = 17
                    }
                    18 -> {
                        property_reference__ = readWithLength { readIrPropertyReference() }
                        oneOfIndex = 18
                    }
                    19 -> {
                        return__ = readWithLength { readIrReturn() }
                        oneOfIndex = 19
                    }
                    20 -> {
                        set_field__ = readWithLength { readIrSetField() }
                        oneOfIndex = 20
                    }
                    21 -> {
                        set_variable__ = readWithLength { readIrSetVariable() }
                        oneOfIndex = 21
                    }
                    22 -> {
                        string_concat__ = readWithLength { readIrStringConcat() }
                        oneOfIndex = 22
                    }
                    23 -> {
                        throw__ = readWithLength { readIrThrow() }
                        oneOfIndex = 23
                    }
                    24 -> {
                        try__ = readWithLength { readIrTry() }
                        oneOfIndex = 24
                    }
                    25 -> {
                        type_op__ = readWithLength { readIrTypeOp() }
                        oneOfIndex = 25
                    }
                    26 -> {
                        vararg__ = readWithLength { readIrVararg() }
                        oneOfIndex = 26
                    }
                    27 -> {
                        when__ = readWithLength { readIrWhen() }
                        oneOfIndex = 27
                    }
                    28 -> {
                        while__ = readWithLength { readIrWhile() }
                        oneOfIndex = 28
                    }
                    29 -> {
                        dynamic_member__ = readWithLength { readIrDynamicMemberExpression() }
                        oneOfIndex = 29
                    }
                    30 -> {
                        dynamic_operator__ = readWithLength { readIrDynamicOperatorExpression() }
                        oneOfIndex = 30
                    }
                    31 -> {
                        local_delegated_property_reference__ = readWithLength { readIrLocalDelegatedPropertyReference() }
                        oneOfIndex = 31
                    }
                    32 -> {
                        constructor_call__ = readWithLength { readIrConstructorCall() }
                        oneOfIndex = 32
                    }
                    33 -> {
                        function_expression__ = readWithLength { readIrFunctionExpression() }
                        oneOfIndex = 33
                    }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            1 -> return createIrOperation_block(block__!!)
            2 -> return createIrOperation_break_(break__!!)
            3 -> return createIrOperation_call(call__!!)
            4 -> return createIrOperation_classReference(class_reference__!!)
            5 -> return createIrOperation_composite(composite__!!)
            6 -> return createIrOperation_const(const__!!)
            7 -> return createIrOperation_continue_(continue__!!)
            8 -> return createIrOperation_delegatingConstructorCall(delegating_constructor_call__!!)
            9 -> return createIrOperation_doWhile(do_while__!!)
            10 -> return createIrOperation_enumConstructorCall(enum_constructor_call__!!)
            11 -> return createIrOperation_functionReference(function_reference__!!)
            12 -> return createIrOperation_getClass(get_class__!!)
            13 -> return createIrOperation_getEnumValue(get_enum_value__!!)
            14 -> return createIrOperation_getField(get_field__!!)
            15 -> return createIrOperation_getObject(get_object__!!)
            16 -> return createIrOperation_getValue(get_value__!!)
            17 -> return createIrOperation_instanceInitializerCall(instance_initializer_call__!!)
            18 -> return createIrOperation_propertyReference(property_reference__!!)
            19 -> return createIrOperation_return_(return__!!)
            20 -> return createIrOperation_setField(set_field__!!)
            21 -> return createIrOperation_setVariable(set_variable__!!)
            22 -> return createIrOperation_stringConcat(string_concat__!!)
            23 -> return createIrOperation_throw_(throw__!!)
            24 -> return createIrOperation_try_(try__!!)
            25 -> return createIrOperation_typeOp(type_op__!!)
            26 -> return createIrOperation_vararg(vararg__!!)
            27 -> return createIrOperation_when_(when__!!)
            28 -> return createIrOperation_while_(while__!!)
            29 -> return createIrOperation_dynamicMember(dynamic_member__!!)
            30 -> return createIrOperation_dynamicOperator(dynamic_operator__!!)
            31 -> return createIrOperation_localDelegatedPropertyReference(local_delegated_property_reference__!!)
            32 -> return createIrOperation_constructorCall(constructor_call__!!)
            33 -> return createIrOperation_functionExpression(function_expression__!!)
            else -> error("Incorrect oneOf index: " + oneOfIndex)
        }
    }

    open fun readIrExpression(): IrExpressionMessageType {
        var operation__: IrOperationMessageType? = null
        var type__: Int? = null
        var coordinates__: CoordinatesMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> operation__ = readWithLength { readIrOperation() }
                    2 -> type__ = readWithLength { readIrDataIndex() }
                    3 -> coordinates__ = readWithLength { readCoordinates() }
                    else -> skip(type)
                }
            }
        }
        return createIrExpression(operation__!!, type__!!, coordinates__!!)
    }

    open fun readNullableIrExpression(): NullableIrExpressionMessageType {
        var expression__: IrExpressionMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> expression__ = readWithLength { readIrExpression() }
                    else -> skip(type)
                }
            }
        }
        return createNullableIrExpression(expression__)
    }

    open fun readIrDeclarationBase(): IrDeclarationBaseMessageType {
        var symbol__: Int? = null
        var origin__: IrDeclarationOriginMessageType? = null
        var coordinates__: CoordinatesMessageType? = null
        var annotations__: List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol__ = readWithLength { readIrDataIndex() }
                    2 -> origin__ = readWithLength { readIrDeclarationOrigin() }
                    3 -> coordinates__ = readWithLength { readCoordinates() }
                    4 -> annotations__ = readWithLength { readAnnotations() }
                    else -> skip(type)
                }
            }
        }
        return createIrDeclarationBase(symbol__!!, origin__!!, coordinates__!!, annotations__!!)
    }

    open fun readIrFunctionBase(): IrFunctionBaseMessageType {
        var base__: IrDeclarationBaseMessageType? = null
        var name__: Int? = null
        var visibility__: VisibilityMessageType? = null
        var is_inline__: Boolean = false
        var is_external__: Boolean = false
        var type_parameters__: List<org.jetbrains.kotlin.ir.declarations.IrTypeParameter>? = null
        var dispatch_receiver__: IrValueParameterMessageType? = null
        var extension_receiver__: IrValueParameterMessageType? = null
        var value_parameter__: MutableList<IrValueParameterMessageType> = mutableListOf()
        var body__: Int? = null
        var return_type__: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> base__ = readWithLength { readIrDeclarationBase() }
                    2 -> name__ = readWithLength { readIrDataIndex() }
                    3 -> visibility__ = readWithLength { readVisibility() }
                    4 -> is_inline__ = readBool()
                    5 -> is_external__ = readBool()
                    6 -> type_parameters__ = readWithLength { readIrTypeParameterContainer() }
                    7 -> dispatch_receiver__ = readWithLength { readIrValueParameter() }
                    8 -> extension_receiver__ = readWithLength { readIrValueParameter() }
                    9 -> value_parameter__.add(readWithLength { readIrValueParameter() })
                    10 -> body__ = readWithLength { readIrDataIndex() }
                    11 -> return_type__ = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrFunctionBase(base__!!, name__!!, visibility__!!, is_inline__, is_external__, type_parameters__!!, dispatch_receiver__, extension_receiver__, value_parameter__, body__, return_type__!!)
    }

    open fun readIrFunction(): IrFunctionMessageType {
        var base__: IrFunctionBaseMessageType? = null
        var modality__: ModalityKindMessageType? = null
        var is_tailrec__: Boolean = false
        var is_suspend__: Boolean = false
        var overridden__: MutableList<Int> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> base__ = readWithLength { readIrFunctionBase() }
                    2 -> modality__ = createModalityKind(readInt32())
                    3 -> is_tailrec__ = readBool()
                    4 -> is_suspend__ = readBool()
                    5 -> overridden__.add(readWithLength { readIrDataIndex() })
                    else -> skip(type)
                }
            }
        }
        return createIrFunction(base__!!, modality__!!, is_tailrec__, is_suspend__, overridden__)
    }

    open fun readIrConstructor(): IrConstructorMessageType {
        var base__: IrFunctionBaseMessageType? = null
        var is_primary__: Boolean = false
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> base__ = readWithLength { readIrFunctionBase() }
                    2 -> is_primary__ = readBool()
                    else -> skip(type)
                }
            }
        }
        return createIrConstructor(base__!!, is_primary__)
    }

    open fun readIrField(): IrFieldMessageType {
        var base__: IrDeclarationBaseMessageType? = null
        var initializer__: Int? = null
        var name__: Int? = null
        var visibility__: VisibilityMessageType? = null
        var is_final__: Boolean = false
        var is_external__: Boolean = false
        var is_static__: Boolean = false
        var type__: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> base__ = readWithLength { readIrDeclarationBase() }
                    2 -> initializer__ = readWithLength { readIrDataIndex() }
                    3 -> name__ = readWithLength { readIrDataIndex() }
                    4 -> visibility__ = readWithLength { readVisibility() }
                    5 -> is_final__ = readBool()
                    6 -> is_external__ = readBool()
                    7 -> is_static__ = readBool()
                    8 -> type__ = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrField(base__!!, initializer__, name__!!, visibility__!!, is_final__, is_external__, is_static__, type__!!)
    }

    open fun readIrLocalDelegatedProperty(): IrLocalDelegatedPropertyMessageType {
        var base__: IrDeclarationBaseMessageType? = null
        var name__: Int? = null
        var type__: Int? = null
        var is_var__: Boolean = false
        var delegate__: IrVariableMessageType? = null
        var getter__: IrFunctionMessageType? = null
        var setter__: IrFunctionMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> base__ = readWithLength { readIrDeclarationBase() }
                    2 -> name__ = readWithLength { readIrDataIndex() }
                    3 -> type__ = readWithLength { readIrDataIndex() }
                    4 -> is_var__ = readBool()
                    5 -> delegate__ = readWithLength { readIrVariable() }
                    6 -> getter__ = readWithLength { readIrFunction() }
                    7 -> setter__ = readWithLength { readIrFunction() }
                    else -> skip(type)
                }
            }
        }
        return createIrLocalDelegatedProperty(base__!!, name__!!, type__!!, is_var__, delegate__!!, getter__, setter__)
    }

    open fun readIrProperty(): IrPropertyMessageType {
        var base__: IrDeclarationBaseMessageType? = null
        var name__: Int? = null
        var visibility__: VisibilityMessageType? = null
        var modality__: ModalityKindMessageType? = null
        var is_var__: Boolean = false
        var is_const__: Boolean = false
        var is_lateinit__: Boolean = false
        var is_delegated__: Boolean = false
        var is_external__: Boolean = false
        var backing_field__: IrFieldMessageType? = null
        var getter__: IrFunctionMessageType? = null
        var setter__: IrFunctionMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> base__ = readWithLength { readIrDeclarationBase() }
                    2 -> name__ = readWithLength { readIrDataIndex() }
                    3 -> visibility__ = readWithLength { readVisibility() }
                    4 -> modality__ = createModalityKind(readInt32())
                    5 -> is_var__ = readBool()
                    6 -> is_const__ = readBool()
                    7 -> is_lateinit__ = readBool()
                    8 -> is_delegated__ = readBool()
                    9 -> is_external__ = readBool()
                    10 -> backing_field__ = readWithLength { readIrField() }
                    11 -> getter__ = readWithLength { readIrFunction() }
                    12 -> setter__ = readWithLength { readIrFunction() }
                    else -> skip(type)
                }
            }
        }
        return createIrProperty(base__!!, name__!!, visibility__!!, modality__!!, is_var__, is_const__, is_lateinit__, is_delegated__, is_external__, backing_field__, getter__, setter__)
    }

    open fun readIrVariable(): IrVariableMessageType {
        var base__: IrDeclarationBaseMessageType? = null
        var name__: Int? = null
        var type__: Int? = null
        var is_var__: Boolean = false
        var is_const__: Boolean = false
        var is_lateinit__: Boolean = false
        var initializer__: IrExpressionMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> base__ = readWithLength { readIrDeclarationBase() }
                    2 -> name__ = readWithLength { readIrDataIndex() }
                    3 -> type__ = readWithLength { readIrDataIndex() }
                    4 -> is_var__ = readBool()
                    5 -> is_const__ = readBool()
                    6 -> is_lateinit__ = readBool()
                    7 -> initializer__ = readWithLength { readIrExpression() }
                    else -> skip(type)
                }
            }
        }
        return createIrVariable(base__!!, name__!!, type__!!, is_var__, is_const__, is_lateinit__, initializer__)
    }

    open fun readIrValueParameter(): IrValueParameterMessageType {
        var base__: IrDeclarationBaseMessageType? = null
        var name__: Int? = null
        var index__: Int = 0
        var type__: Int? = null
        var vararg_element_type__: Int? = null
        var is_crossinline__: Boolean = false
        var is_noinline__: Boolean = false
        var default_value__: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> base__ = readWithLength { readIrDeclarationBase() }
                    2 -> name__ = readWithLength { readIrDataIndex() }
                    3 -> index__ = readInt32()
                    4 -> type__ = readWithLength { readIrDataIndex() }
                    5 -> vararg_element_type__ = readWithLength { readIrDataIndex() }
                    6 -> is_crossinline__ = readBool()
                    7 -> is_noinline__ = readBool()
                    8 -> default_value__ = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrValueParameter(base__!!, name__!!, index__, type__!!, vararg_element_type__, is_crossinline__, is_noinline__, default_value__)
    }

    open fun readIrTypeParameter(): IrTypeParameterMessageType {
        var base__: IrDeclarationBaseMessageType? = null
        var name__: Int? = null
        var index__: Int = 0
        var variance__: IrTypeVarianceMessageType? = null
        var super_type__: MutableList<Int> = mutableListOf()
        var is_reified__: Boolean = false
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> base__ = readWithLength { readIrDeclarationBase() }
                    2 -> name__ = readWithLength { readIrDataIndex() }
                    3 -> index__ = readInt32()
                    4 -> variance__ = createIrTypeVariance(readInt32())
                    5 -> super_type__.add(readWithLength { readIrDataIndex() })
                    6 -> is_reified__ = readBool()
                    else -> skip(type)
                }
            }
        }
        return createIrTypeParameter(base__!!, name__!!, index__, variance__!!, super_type__, is_reified__)
    }

    open fun readIrTypeParameterContainer(): List<org.jetbrains.kotlin.ir.declarations.IrTypeParameter> {
        var type_parameter__: MutableList<IrTypeParameterMessageType> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> type_parameter__.add(readWithLength { readIrTypeParameter() })
                    else -> skip(type)
                }
            }
        }
        return createIrTypeParameterContainer(type_parameter__)
    }

    open fun readIrClass(): IrClassMessageType {
        var base__: IrDeclarationBaseMessageType? = null
        var name__: Int? = null
        var kind__: ClassKindMessageType? = null
        var visibility__: VisibilityMessageType? = null
        var modality__: ModalityKindMessageType? = null
        var is_companion__: Boolean = false
        var is_inner__: Boolean = false
        var is_data__: Boolean = false
        var is_external__: Boolean = false
        var is_inline__: Boolean = false
        var this_receiver__: IrValueParameterMessageType? = null
        var type_parameters__: List<org.jetbrains.kotlin.ir.declarations.IrTypeParameter>? = null
        var declaration_container__: List<org.jetbrains.kotlin.ir.declarations.IrDeclaration>? = null
        var super_type__: MutableList<Int> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> base__ = readWithLength { readIrDeclarationBase() }
                    2 -> name__ = readWithLength { readIrDataIndex() }
                    3 -> kind__ = createClassKind(readInt32())
                    4 -> visibility__ = readWithLength { readVisibility() }
                    5 -> modality__ = createModalityKind(readInt32())
                    6 -> is_companion__ = readBool()
                    7 -> is_inner__ = readBool()
                    8 -> is_data__ = readBool()
                    9 -> is_external__ = readBool()
                    10 -> is_inline__ = readBool()
                    11 -> this_receiver__ = readWithLength { readIrValueParameter() }
                    12 -> type_parameters__ = readWithLength { readIrTypeParameterContainer() }
                    13 -> declaration_container__ = readWithLength { readIrDeclarationContainer() }
                    14 -> super_type__.add(readWithLength { readIrDataIndex() })
                    else -> skip(type)
                }
            }
        }
        return createIrClass(base__!!, name__!!, kind__!!, visibility__!!, modality__!!, is_companion__, is_inner__, is_data__, is_external__, is_inline__, this_receiver__, type_parameters__!!, declaration_container__!!, super_type__)
    }

    open fun readIrTypeAlias(): IrTypeAliasMessageType {
        var base__: IrDeclarationBaseMessageType? = null
        var name__: Int? = null
        var visibility__: VisibilityMessageType? = null
        var type_parameters__: List<org.jetbrains.kotlin.ir.declarations.IrTypeParameter>? = null
        var expanded_type__: Int? = null
        var is_actual__: Boolean = false
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> base__ = readWithLength { readIrDeclarationBase() }
                    2 -> name__ = readWithLength { readIrDataIndex() }
                    3 -> visibility__ = readWithLength { readVisibility() }
                    4 -> type_parameters__ = readWithLength { readIrTypeParameterContainer() }
                    5 -> expanded_type__ = readWithLength { readIrDataIndex() }
                    6 -> is_actual__ = readBool()
                    else -> skip(type)
                }
            }
        }
        return createIrTypeAlias(base__!!, name__!!, visibility__!!, type_parameters__!!, expanded_type__!!, is_actual__)
    }

    open fun readIrEnumEntry(): IrEnumEntryMessageType {
        var base__: IrDeclarationBaseMessageType? = null
        var initializer__: Int? = null
        var corresponding_class__: IrClassMessageType? = null
        var name__: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> base__ = readWithLength { readIrDeclarationBase() }
                    2 -> initializer__ = readWithLength { readIrDataIndex() }
                    3 -> corresponding_class__ = readWithLength { readIrClass() }
                    4 -> name__ = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrEnumEntry(base__!!, initializer__, corresponding_class__, name__!!)
    }

    open fun readIrAnonymousInit(): IrAnonymousInitMessageType {
        var base__: IrDeclarationBaseMessageType? = null
        var body__: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> base__ = readWithLength { readIrDeclarationBase() }
                    2 -> body__ = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrAnonymousInit(base__!!, body__!!)
    }

    open fun readIrDeclaration(): IrDeclarationMessageType {
        var ir_anonymous_init__: IrAnonymousInitMessageType? = null
        var ir_class__: IrClassMessageType? = null
        var ir_constructor__: IrConstructorMessageType? = null
        var ir_enum_entry__: IrEnumEntryMessageType? = null
        var ir_field__: IrFieldMessageType? = null
        var ir_function__: IrFunctionMessageType? = null
        var ir_property__: IrPropertyMessageType? = null
        var ir_type_parameter__: IrTypeParameterMessageType? = null
        var ir_variable__: IrVariableMessageType? = null
        var ir_value_parameter__: IrValueParameterMessageType? = null
        var ir_local_delegated_property__: IrLocalDelegatedPropertyMessageType? = null
        var ir_type_alias__: IrTypeAliasMessageType? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> {
                        ir_anonymous_init__ = readWithLength { readIrAnonymousInit() }
                        oneOfIndex = 1
                    }
                    2 -> {
                        ir_class__ = readWithLength { readIrClass() }
                        oneOfIndex = 2
                    }
                    3 -> {
                        ir_constructor__ = readWithLength { readIrConstructor() }
                        oneOfIndex = 3
                    }
                    4 -> {
                        ir_enum_entry__ = readWithLength { readIrEnumEntry() }
                        oneOfIndex = 4
                    }
                    5 -> {
                        ir_field__ = readWithLength { readIrField() }
                        oneOfIndex = 5
                    }
                    6 -> {
                        ir_function__ = readWithLength { readIrFunction() }
                        oneOfIndex = 6
                    }
                    7 -> {
                        ir_property__ = readWithLength { readIrProperty() }
                        oneOfIndex = 7
                    }
                    8 -> {
                        ir_type_parameter__ = readWithLength { readIrTypeParameter() }
                        oneOfIndex = 8
                    }
                    9 -> {
                        ir_variable__ = readWithLength { readIrVariable() }
                        oneOfIndex = 9
                    }
                    10 -> {
                        ir_value_parameter__ = readWithLength { readIrValueParameter() }
                        oneOfIndex = 10
                    }
                    11 -> {
                        ir_local_delegated_property__ = readWithLength { readIrLocalDelegatedProperty() }
                        oneOfIndex = 11
                    }
                    12 -> {
                        ir_type_alias__ = readWithLength { readIrTypeAlias() }
                        oneOfIndex = 12
                    }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            1 -> return createIrDeclaration_irAnonymousInit(ir_anonymous_init__!!)
            2 -> return createIrDeclaration_irClass(ir_class__!!)
            3 -> return createIrDeclaration_irConstructor(ir_constructor__!!)
            4 -> return createIrDeclaration_irEnumEntry(ir_enum_entry__!!)
            5 -> return createIrDeclaration_irField(ir_field__!!)
            6 -> return createIrDeclaration_irFunction(ir_function__!!)
            7 -> return createIrDeclaration_irProperty(ir_property__!!)
            8 -> return createIrDeclaration_irTypeParameter(ir_type_parameter__!!)
            9 -> return createIrDeclaration_irVariable(ir_variable__!!)
            10 -> return createIrDeclaration_irValueParameter(ir_value_parameter__!!)
            11 -> return createIrDeclaration_irLocalDelegatedProperty(ir_local_delegated_property__!!)
            12 -> return createIrDeclaration_irTypeAlias(ir_type_alias__!!)
            else -> error("Incorrect oneOf index: " + oneOfIndex)
        }
    }

    open fun readIrBranch(): IrBranchMessageType {
        var condition__: IrExpressionMessageType? = null
        var result__: IrExpressionMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> condition__ = readWithLength { readIrExpression() }
                    2 -> result__ = readWithLength { readIrExpression() }
                    else -> skip(type)
                }
            }
        }
        return createIrBranch(condition__!!, result__!!)
    }

    open fun readIrBlockBody(): IrBlockBodyMessageType {
        var statement__: MutableList<IrStatementMessageType> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> statement__.add(readWithLength { readIrStatement() })
                    else -> skip(type)
                }
            }
        }
        return createIrBlockBody(statement__)
    }

    open fun readIrCatch(): IrCatchMessageType {
        var catch_parameter__: IrVariableMessageType? = null
        var result__: IrExpressionMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> catch_parameter__ = readWithLength { readIrVariable() }
                    2 -> result__ = readWithLength { readIrExpression() }
                    else -> skip(type)
                }
            }
        }
        return createIrCatch(catch_parameter__!!, result__!!)
    }

    open fun readIrSyntheticBody(): IrSyntheticBodyMessageType {
        var kind__: IrSyntheticBodyKindMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> kind__ = createIrSyntheticBodyKind(readInt32())
                    else -> skip(type)
                }
            }
        }
        return createIrSyntheticBody(kind__!!)
    }

    open fun readIrStatement(): IrStatementMessageType {
        var coordinates__: CoordinatesMessageType? = null
        var declaration__: IrDeclarationMessageType? = null
        var expression__: IrExpressionMessageType? = null
        var block_body__: IrBlockBodyMessageType? = null
        var branch__: IrBranchMessageType? = null
        var catch__: IrCatchMessageType? = null
        var synthetic_body__: IrSyntheticBodyMessageType? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> coordinates__ = readWithLength { readCoordinates() }
                    2 -> {
                        declaration__ = readWithLength { readIrDeclaration() }
                        oneOfIndex = 2
                    }
                    3 -> {
                        expression__ = readWithLength { readIrExpression() }
                        oneOfIndex = 3
                    }
                    4 -> {
                        block_body__ = readWithLength { readIrBlockBody() }
                        oneOfIndex = 4
                    }
                    5 -> {
                        branch__ = readWithLength { readIrBranch() }
                        oneOfIndex = 5
                    }
                    6 -> {
                        catch__ = readWithLength { readIrCatch() }
                        oneOfIndex = 6
                    }
                    7 -> {
                        synthetic_body__ = readWithLength { readIrSyntheticBody() }
                        oneOfIndex = 7
                    }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            2 -> return createIrStatement_declaration(coordinates__!!, declaration__!!)
            3 -> return createIrStatement_expression(coordinates__!!, expression__!!)
            4 -> return createIrStatement_blockBody(coordinates__!!, block_body__!!)
            5 -> return createIrStatement_branch(coordinates__!!, branch__!!)
            6 -> return createIrStatement_catch(coordinates__!!, catch__!!)
            7 -> return createIrStatement_syntheticBody(coordinates__!!, synthetic_body__!!)
            else -> error("Incorrect oneOf index: " + oneOfIndex)
        }
    }

}