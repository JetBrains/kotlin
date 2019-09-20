/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.nextgen

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor as DescriptorReferenceMessageType
import org.jetbrains.kotlin.backend.common.serialization.UniqId as UniqIdMessageType
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
import org.jetbrains.kotlin.ir.expressions.IrGetField as IrGetFieldMessageType
import org.jetbrains.kotlin.ir.expressions.IrGetValue as IrGetValueMessageType
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue as IrGetObjectMessageType
import org.jetbrains.kotlin.ir.expressions.IrInstanceInitializerCall as IrInstanceInitializerCallMessageType
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
import org.jetbrains.kotlin.backend.common.serialization.nextgen.NullableExpression as NullableIrExpressionMessageType
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
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBody as IrSyntheticBodyMessageType
import org.jetbrains.kotlin.ir.IrElement as IrStatementMessageType

abstract class AbstractIrSmartProtoReader(source: ByteArray) : ProtoReader(source) {
    abstract fun createDescriptorReference(packageFqName : FqNameMessageType, classFqName : FqNameMessageType, name : Int, uniqId : UniqIdMessageType?, isGetter : Boolean?, isSetter : Boolean?, isBackingField : Boolean?, isFakeOverride : Boolean?, isDefaultConstructor : Boolean?, isEnumEntry : Boolean?, isEnumSpecial : Boolean?, isTypeParameter : Boolean?): DescriptorReferenceMessageType

    abstract fun createUniqId(index : Long, isLocal : Boolean): UniqIdMessageType

    abstract fun createVisibility(name : Int): VisibilityMessageType

    abstract fun createIrStatementOrigin(name : Int): IrStatementOriginMessageType

    abstract fun createKnownOrigin(index: Int): KnownOriginMessageType

    abstract fun createIrDeclarationOrigin_origin(oneOfOrigin : KnownOriginMessageType): IrDeclarationOriginMessageType
    abstract fun createIrDeclarationOrigin_custom(oneOfCustom : Int): IrDeclarationOriginMessageType

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

    abstract fun createIrTypeProjection(variance : IrTypeVarianceMessageType, type_ : Int): IrTypeProjectionMessageType

    abstract fun createIrTypeArgument_star(oneOfStar : IrStarProjectionMessageType): IrTypeArgumentMessageType
    abstract fun createIrTypeArgument_type_(oneOfType : IrTypeProjectionMessageType): IrTypeArgumentMessageType

    abstract fun createIrSimpleType(annotations : List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>, classifier : Int, hasQuestionMark : Boolean, argument : List<IrTypeArgumentMessageType>, abbreviation : IrTypeAbbreviationMessageType?): IrSimpleTypeMessageType

    abstract fun createIrTypeAbbreviation(annotations : List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>, typeAlias : Int, hasQuestionMark : Boolean, argument : List<IrTypeArgumentMessageType>): IrTypeAbbreviationMessageType

    abstract fun createIrDynamicType(annotations : List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>): IrDynamicTypeMessageType

    abstract fun createIrErrorType(annotations : List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>): IrErrorTypeMessageType

    abstract fun createIrType_simple(oneOfSimple : IrSimpleTypeMessageType): IrTypeMessageType
    abstract fun createIrType_dynamic(oneOfDynamic : IrDynamicTypeMessageType): IrTypeMessageType
    abstract fun createIrType_error(oneOfError : IrErrorTypeMessageType): IrTypeMessageType

    abstract fun createIrTypeTable(types : List<IrTypeMessageType>): Array<org.jetbrains.kotlin.ir.types.IrType>

    abstract fun createIrBreak(loopId : Int, label : Int?): IrBreakMessageType

    abstract fun createIrBlock(origin : IrStatementOriginMessageType?, statement : List<IrStatementMessageType>): IrBlockMessageType

    abstract fun createIrCall(symbol : Int, memberAccessDispatchReceiver : IrExpressionMessageType?, memberAccessExtensionReceiver : IrExpressionMessageType?, memberAccessValueArgument : List<NullableIrExpressionMessageType>, memberAccessTypeArguments : List<org.jetbrains.kotlin.ir.types.IrType>, super_ : Int?, origin : IrStatementOriginMessageType?): IrCallMessageType

    abstract fun createIrConstructorCall(symbol : Int, constructorTypeArgumentsCount : Int, memberAccessDispatchReceiver : IrExpressionMessageType?, memberAccessExtensionReceiver : IrExpressionMessageType?, memberAccessValueArgument : List<NullableIrExpressionMessageType>, memberAccessTypeArguments : List<org.jetbrains.kotlin.ir.types.IrType>): IrConstructorCallMessageType

    abstract fun createIrFunctionReference(symbol : Int, origin : IrStatementOriginMessageType?, memberAccessDispatchReceiver : IrExpressionMessageType?, memberAccessExtensionReceiver : IrExpressionMessageType?, memberAccessValueArgument : List<NullableIrExpressionMessageType>, memberAccessTypeArguments : List<org.jetbrains.kotlin.ir.types.IrType>): IrFunctionReferenceMessageType

    abstract fun createIrLocalDelegatedPropertyReference(delegate : Int, getter : Int?, setter : Int?, symbol : Int, origin : IrStatementOriginMessageType?): IrLocalDelegatedPropertyReferenceMessageType

    abstract fun createIrPropertyReference(field : Int?, getter : Int?, setter : Int?, origin : IrStatementOriginMessageType?, memberAccessDispatchReceiver : IrExpressionMessageType?, memberAccessExtensionReceiver : IrExpressionMessageType?, memberAccessValueArgument : List<NullableIrExpressionMessageType>, memberAccessTypeArguments : List<org.jetbrains.kotlin.ir.types.IrType>, symbol : Int): IrPropertyReferenceMessageType

    abstract fun createIrComposite(statement : List<IrStatementMessageType>, origin : IrStatementOriginMessageType?): IrCompositeMessageType

    abstract fun createIrClassReference(classSymbol : Int, classType : Int): IrClassReferenceMessageType

    abstract fun createIrConst_null_(oneOfNull : Boolean): IrConstMessageType<*>
    abstract fun createIrConst_boolean(oneOfBoolean : Boolean): IrConstMessageType<*>
    abstract fun createIrConst_char(oneOfChar : Int): IrConstMessageType<*>
    abstract fun createIrConst_byte(oneOfByte : Int): IrConstMessageType<*>
    abstract fun createIrConst_short(oneOfShort : Int): IrConstMessageType<*>
    abstract fun createIrConst_int(oneOfInt : Int): IrConstMessageType<*>
    abstract fun createIrConst_long(oneOfLong : Long): IrConstMessageType<*>
    abstract fun createIrConst_float(oneOfFloat : Float): IrConstMessageType<*>
    abstract fun createIrConst_double(oneOfDouble : Double): IrConstMessageType<*>
    abstract fun createIrConst_string(oneOfString : Int): IrConstMessageType<*>

    abstract fun createIrContinue(loopId : Int, label : Int?): IrContinueMessageType

    abstract fun createIrDelegatingConstructorCall(symbol : Int, memberAccessDispatchReceiver : IrExpressionMessageType?, memberAccessExtensionReceiver : IrExpressionMessageType?, memberAccessValueArgument : List<NullableIrExpressionMessageType>, memberAccessTypeArguments : List<org.jetbrains.kotlin.ir.types.IrType>): IrDelegatingConstructorCallMessageType

    abstract fun createIrDoWhile(loopLoopId : Int, loopCondition : IrExpressionMessageType, loopLabel : Int?, loopOrigin : IrStatementOriginMessageType?): IrDoWhileMessageType
    abstract fun createIrDoWhile1(partial: IrDoWhileMessageType, loopBody : IrExpressionMessageType?): IrDoWhileMessageType

    abstract fun createIrEnumConstructorCall(symbol : Int, memberAccessDispatchReceiver : IrExpressionMessageType?, memberAccessExtensionReceiver : IrExpressionMessageType?, memberAccessValueArgument : List<NullableIrExpressionMessageType>, memberAccessTypeArguments : List<org.jetbrains.kotlin.ir.types.IrType>): IrEnumConstructorCallMessageType

    abstract fun createIrGetClass(argument : IrExpressionMessageType): IrGetClassMessageType

    abstract fun createIrGetEnumValue(symbol : Int): IrGetEnumValueMessageType

    abstract fun createIrGetField(fieldAccessSymbol : Int, fieldAccessSuper : Int?, fieldAccessReceiver : IrExpressionMessageType?, origin : IrStatementOriginMessageType?): IrGetFieldMessageType

    abstract fun createIrGetValue(symbol : Int, origin : IrStatementOriginMessageType?): IrGetValueMessageType

    abstract fun createIrGetObject(symbol : Int): IrGetObjectMessageType

    abstract fun createIrInstanceInitializerCall(symbol : Int): IrInstanceInitializerCallMessageType

    abstract fun createIrReturn(returnTarget : Int, value : IrExpressionMessageType): IrReturnMessageType

    abstract fun createIrSetField(fieldAccessSymbol : Int, fieldAccessSuper : Int?, fieldAccessReceiver : IrExpressionMessageType?, value : IrExpressionMessageType, origin : IrStatementOriginMessageType?): IrSetFieldMessageType

    abstract fun createIrSetVariable(symbol : Int, value : IrExpressionMessageType, origin : IrStatementOriginMessageType?): IrSetVariableMessageType

    abstract fun createIrSpreadElement(expression : IrExpressionMessageType, coordinatesStartOffset : Int, coordinatesEndOffset : Int): IrSpreadElementMessageType

    abstract fun createIrStringConcat(argument : List<IrExpressionMessageType>): IrStringConcatMessageType

    abstract fun createIrThrow(value : IrExpressionMessageType): IrThrowMessageType

    abstract fun createIrTry(result : IrExpressionMessageType, catch : List<IrStatementMessageType>, finally : IrExpressionMessageType?): IrTryMessageType

    abstract fun createIrTypeOp(operator : IrTypeOperatorMessageType, operand : Int, argument : IrExpressionMessageType): IrTypeOpMessageType

    abstract fun createIrVararg(elementType : Int, element : List<IrVarargElementMessageType>): IrVarargMessageType

    abstract fun createIrVarargElement_expression(oneOfExpression : IrExpressionMessageType): IrVarargElementMessageType
    abstract fun createIrVarargElement_spreadElement(oneOfSpreadElement : IrSpreadElementMessageType): IrVarargElementMessageType

    abstract fun createIrWhen(branch : List<IrStatementMessageType>, origin : IrStatementOriginMessageType?): IrWhenMessageType

    abstract fun createIrWhile(loopLoopId : Int, loopCondition : IrExpressionMessageType, loopLabel : Int?, loopOrigin : IrStatementOriginMessageType?): IrWhileMessageType
    abstract fun createIrWhile1(partial: IrWhileMessageType, loopBody : IrExpressionMessageType?): IrWhileMessageType

    abstract fun createIrFunctionExpression(function : IrFunctionMessageType, origin : IrStatementOriginMessageType): IrFunctionExpressionMessageType

    abstract fun createIrDynamicMemberExpression(memberName : Int, receiver : IrExpressionMessageType): IrDynamicMemberExpressionMessageType

    abstract fun createIrDynamicOperator(index: Int): IrDynamicOperatorMessageType

    abstract fun createIrDynamicOperatorExpression(operator : IrDynamicOperatorMessageType, receiver : IrExpressionMessageType, argument : List<IrExpressionMessageType>): IrDynamicOperatorExpressionMessageType

    abstract fun createIrOperation_block(oneOfBlock : IrBlockMessageType): IrOperationMessageType
    abstract fun createIrOperation_break_(oneOfBreak : IrBreakMessageType): IrOperationMessageType
    abstract fun createIrOperation_call(oneOfCall : IrCallMessageType): IrOperationMessageType
    abstract fun createIrOperation_classReference(oneOfClassReference : IrClassReferenceMessageType): IrOperationMessageType
    abstract fun createIrOperation_composite(oneOfComposite : IrCompositeMessageType): IrOperationMessageType
    abstract fun createIrOperation_const(oneOfConst : IrConstMessageType<*>): IrOperationMessageType
    abstract fun createIrOperation_continue_(oneOfContinue : IrContinueMessageType): IrOperationMessageType
    abstract fun createIrOperation_delegatingConstructorCall(oneOfDelegatingConstructorCall : IrDelegatingConstructorCallMessageType): IrOperationMessageType
    abstract fun createIrOperation_doWhile(oneOfDoWhile : IrDoWhileMessageType): IrOperationMessageType
    abstract fun createIrOperation_enumConstructorCall(oneOfEnumConstructorCall : IrEnumConstructorCallMessageType): IrOperationMessageType
    abstract fun createIrOperation_functionReference(oneOfFunctionReference : IrFunctionReferenceMessageType): IrOperationMessageType
    abstract fun createIrOperation_getClass(oneOfGetClass : IrGetClassMessageType): IrOperationMessageType
    abstract fun createIrOperation_getEnumValue(oneOfGetEnumValue : IrGetEnumValueMessageType): IrOperationMessageType
    abstract fun createIrOperation_getField(oneOfGetField : IrGetFieldMessageType): IrOperationMessageType
    abstract fun createIrOperation_getObject(oneOfGetObject : IrGetObjectMessageType): IrOperationMessageType
    abstract fun createIrOperation_getValue(oneOfGetValue : IrGetValueMessageType): IrOperationMessageType
    abstract fun createIrOperation_instanceInitializerCall(oneOfInstanceInitializerCall : IrInstanceInitializerCallMessageType): IrOperationMessageType
    abstract fun createIrOperation_propertyReference(oneOfPropertyReference : IrPropertyReferenceMessageType): IrOperationMessageType
    abstract fun createIrOperation_return_(oneOfReturn : IrReturnMessageType): IrOperationMessageType
    abstract fun createIrOperation_setField(oneOfSetField : IrSetFieldMessageType): IrOperationMessageType
    abstract fun createIrOperation_setVariable(oneOfSetVariable : IrSetVariableMessageType): IrOperationMessageType
    abstract fun createIrOperation_stringConcat(oneOfStringConcat : IrStringConcatMessageType): IrOperationMessageType
    abstract fun createIrOperation_throw_(oneOfThrow : IrThrowMessageType): IrOperationMessageType
    abstract fun createIrOperation_try_(oneOfTry : IrTryMessageType): IrOperationMessageType
    abstract fun createIrOperation_typeOp(oneOfTypeOp : IrTypeOpMessageType): IrOperationMessageType
    abstract fun createIrOperation_vararg(oneOfVararg : IrVarargMessageType): IrOperationMessageType
    abstract fun createIrOperation_when_(oneOfWhen : IrWhenMessageType): IrOperationMessageType
    abstract fun createIrOperation_while_(oneOfWhile : IrWhileMessageType): IrOperationMessageType
    abstract fun createIrOperation_dynamicMember(oneOfDynamicMember : IrDynamicMemberExpressionMessageType): IrOperationMessageType
    abstract fun createIrOperation_dynamicOperator(oneOfDynamicOperator : IrDynamicOperatorExpressionMessageType): IrOperationMessageType
    abstract fun createIrOperation_localDelegatedPropertyReference(oneOfLocalDelegatedPropertyReference : IrLocalDelegatedPropertyReferenceMessageType): IrOperationMessageType
    abstract fun createIrOperation_constructorCall(oneOfConstructorCall : IrConstructorCallMessageType): IrOperationMessageType
    abstract fun createIrOperation_functionExpression(oneOfFunctionExpression : IrFunctionExpressionMessageType): IrOperationMessageType

    abstract fun createIrTypeOperator(index: Int): IrTypeOperatorMessageType

    abstract fun createIrExpression(operation : IrOperationMessageType, type_ : Int, coordinatesStartOffset : Int, coordinatesEndOffset : Int): IrExpressionMessageType

    abstract fun createNullableIrExpression(expression : IrExpressionMessageType?): NullableIrExpressionMessageType

    abstract fun createIrFunction(baseBaseSymbol : Int, baseBaseOrigin : IrDeclarationOriginMessageType, baseBaseCoordinatesStartOffset : Int, baseBaseCoordinatesEndOffset : Int, baseBaseAnnotations : List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>, baseName : Int, baseVisibility : VisibilityMessageType, baseIsInline : Boolean, baseIsExternal : Boolean, baseReturnType : Int, modality : ModalityKindMessageType, isTailrec : Boolean, isSuspend : Boolean, overridden : List<Int>): IrFunctionMessageType
    abstract fun createIrFunction1(partial: IrFunctionMessageType, baseTypeParameters : List<org.jetbrains.kotlin.ir.declarations.IrTypeParameter>, baseDispatchReceiver : IrValueParameterMessageType?, baseExtensionReceiver : IrValueParameterMessageType?, baseValueParameter : List<IrValueParameterMessageType>, baseBody : Int?): IrFunctionMessageType

    abstract fun createIrConstructor(baseBaseSymbol : Int, baseBaseOrigin : IrDeclarationOriginMessageType, baseBaseCoordinatesStartOffset : Int, baseBaseCoordinatesEndOffset : Int, baseBaseAnnotations : List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>, baseName : Int, baseVisibility : VisibilityMessageType, baseIsInline : Boolean, baseIsExternal : Boolean, baseReturnType : Int, isPrimary : Boolean): IrConstructorMessageType
    abstract fun createIrConstructor1(partial: IrConstructorMessageType, baseTypeParameters : List<org.jetbrains.kotlin.ir.declarations.IrTypeParameter>, baseDispatchReceiver : IrValueParameterMessageType?, baseExtensionReceiver : IrValueParameterMessageType?, baseValueParameter : List<IrValueParameterMessageType>, baseBody : Int?): IrConstructorMessageType

    abstract fun createIrField(baseSymbol : Int, baseOrigin : IrDeclarationOriginMessageType, baseCoordinatesStartOffset : Int, baseCoordinatesEndOffset : Int, baseAnnotations : List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>, initializer : Int?, name : Int, visibility : VisibilityMessageType, isFinal : Boolean, isExternal : Boolean, isStatic : Boolean, type_ : Int): IrFieldMessageType

    abstract fun createIrLocalDelegatedProperty(baseSymbol : Int, baseOrigin : IrDeclarationOriginMessageType, baseCoordinatesStartOffset : Int, baseCoordinatesEndOffset : Int, baseAnnotations : List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>, name : Int, type_ : Int, isVar : Boolean, delegate : IrVariableMessageType, getter : IrFunctionMessageType?, setter : IrFunctionMessageType?): IrLocalDelegatedPropertyMessageType

    abstract fun createIrProperty(baseSymbol : Int, baseOrigin : IrDeclarationOriginMessageType, baseCoordinatesStartOffset : Int, baseCoordinatesEndOffset : Int, baseAnnotations : List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>, name : Int, visibility : VisibilityMessageType, modality : ModalityKindMessageType, isVar : Boolean, isConst : Boolean, isLateinit : Boolean, isDelegated : Boolean, isExternal : Boolean, backingField : IrFieldMessageType?, getter : IrFunctionMessageType?, setter : IrFunctionMessageType?): IrPropertyMessageType

    abstract fun createIrVariable(baseSymbol : Int, baseOrigin : IrDeclarationOriginMessageType, baseCoordinatesStartOffset : Int, baseCoordinatesEndOffset : Int, baseAnnotations : List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>, name : Int, type_ : Int, isVar : Boolean, isConst : Boolean, isLateinit : Boolean, initializer : IrExpressionMessageType?): IrVariableMessageType

    abstract fun createClassKind(index: Int): ClassKindMessageType

    abstract fun createModalityKind(index: Int): ModalityKindMessageType

    abstract fun createIrValueParameter(baseSymbol : Int, baseOrigin : IrDeclarationOriginMessageType, baseCoordinatesStartOffset : Int, baseCoordinatesEndOffset : Int, baseAnnotations : List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>, name : Int, index : Int, type_ : Int, varargElementType : Int?, isCrossinline : Boolean, isNoinline : Boolean, defaultValue : Int?): IrValueParameterMessageType

    abstract fun createIrTypeParameter(baseSymbol : Int, baseOrigin : IrDeclarationOriginMessageType, baseCoordinatesStartOffset : Int, baseCoordinatesEndOffset : Int, baseAnnotations : List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>, name : Int, index : Int, variance : IrTypeVarianceMessageType, superType : List<Int>, isReified : Boolean): IrTypeParameterMessageType

    abstract fun createIrTypeParameterContainer(typeParameter : List<IrTypeParameterMessageType>): List<org.jetbrains.kotlin.ir.declarations.IrTypeParameter>

    abstract fun createIrClass(baseSymbol : Int, baseOrigin : IrDeclarationOriginMessageType, baseCoordinatesStartOffset : Int, baseCoordinatesEndOffset : Int, baseAnnotations : List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>, name : Int, kind : ClassKindMessageType, visibility : VisibilityMessageType, modality : ModalityKindMessageType, isCompanion : Boolean, isInner : Boolean, isData : Boolean, isExternal : Boolean, isInline : Boolean, superType : List<Int>): IrClassMessageType
    abstract fun createIrClass1(partial: IrClassMessageType, thisReceiver : IrValueParameterMessageType?, typeParameters : List<org.jetbrains.kotlin.ir.declarations.IrTypeParameter>): IrClassMessageType
    abstract fun createIrClass2(partial: IrClassMessageType, declarationContainer : List<org.jetbrains.kotlin.ir.declarations.IrDeclaration>): IrClassMessageType

    abstract fun createIrTypeAlias(baseSymbol : Int, baseOrigin : IrDeclarationOriginMessageType, baseCoordinatesStartOffset : Int, baseCoordinatesEndOffset : Int, baseAnnotations : List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>, name : Int, visibility : VisibilityMessageType, typeParameters : List<org.jetbrains.kotlin.ir.declarations.IrTypeParameter>, expandedType : Int, isActual : Boolean): IrTypeAliasMessageType

    abstract fun createIrEnumEntry(baseSymbol : Int, baseOrigin : IrDeclarationOriginMessageType, baseCoordinatesStartOffset : Int, baseCoordinatesEndOffset : Int, baseAnnotations : List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>, initializer : Int?, correspondingClass : IrClassMessageType?, name : Int): IrEnumEntryMessageType

    abstract fun createIrAnonymousInit(baseSymbol : Int, baseOrigin : IrDeclarationOriginMessageType, baseCoordinatesStartOffset : Int, baseCoordinatesEndOffset : Int, baseAnnotations : List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>, body : Int): IrAnonymousInitMessageType

    abstract fun createIrDeclaration_irAnonymousInit(oneOfIrAnonymousInit : IrAnonymousInitMessageType): IrDeclarationMessageType
    abstract fun createIrDeclaration_irClass(oneOfIrClass : IrClassMessageType): IrDeclarationMessageType
    abstract fun createIrDeclaration_irConstructor(oneOfIrConstructor : IrConstructorMessageType): IrDeclarationMessageType
    abstract fun createIrDeclaration_irEnumEntry(oneOfIrEnumEntry : IrEnumEntryMessageType): IrDeclarationMessageType
    abstract fun createIrDeclaration_irField(oneOfIrField : IrFieldMessageType): IrDeclarationMessageType
    abstract fun createIrDeclaration_irFunction(oneOfIrFunction : IrFunctionMessageType): IrDeclarationMessageType
    abstract fun createIrDeclaration_irProperty(oneOfIrProperty : IrPropertyMessageType): IrDeclarationMessageType
    abstract fun createIrDeclaration_irTypeParameter(oneOfIrTypeParameter : IrTypeParameterMessageType): IrDeclarationMessageType
    abstract fun createIrDeclaration_irVariable(oneOfIrVariable : IrVariableMessageType): IrDeclarationMessageType
    abstract fun createIrDeclaration_irValueParameter(oneOfIrValueParameter : IrValueParameterMessageType): IrDeclarationMessageType
    abstract fun createIrDeclaration_irLocalDelegatedProperty(oneOfIrLocalDelegatedProperty : IrLocalDelegatedPropertyMessageType): IrDeclarationMessageType
    abstract fun createIrDeclaration_irTypeAlias(oneOfIrTypeAlias : IrTypeAliasMessageType): IrDeclarationMessageType

    abstract fun createIrBranch(condition : IrExpressionMessageType, result : IrExpressionMessageType): IrBranchMessageType

    abstract fun createIrBlockBody(statement : List<IrStatementMessageType>): IrBlockBodyMessageType

    abstract fun createIrCatch(catchParameter : IrVariableMessageType, result : IrExpressionMessageType): IrCatchMessageType

    abstract fun createIrSyntheticBodyKind(index: Int): IrSyntheticBodyKindMessageType

    abstract fun createIrSyntheticBody(kind : IrSyntheticBodyKindMessageType): IrSyntheticBodyMessageType

    abstract fun createIrStatement_declaration(coordinatesStartOffset : Int, coordinatesEndOffset : Int, oneOfDeclaration : IrDeclarationMessageType): IrStatementMessageType
    abstract fun createIrStatement_expression(coordinatesStartOffset : Int, coordinatesEndOffset : Int, oneOfExpression : IrExpressionMessageType): IrStatementMessageType
    abstract fun createIrStatement_blockBody(coordinatesStartOffset : Int, coordinatesEndOffset : Int, oneOfBlockBody : IrBlockBodyMessageType): IrStatementMessageType
    abstract fun createIrStatement_branch(coordinatesStartOffset : Int, coordinatesEndOffset : Int, oneOfBranch : IrBranchMessageType): IrStatementMessageType
    abstract fun createIrStatement_catch(coordinatesStartOffset : Int, coordinatesEndOffset : Int, oneOfCatch : IrCatchMessageType): IrStatementMessageType
    abstract fun createIrStatement_syntheticBody(coordinatesStartOffset : Int, coordinatesEndOffset : Int, oneOfSyntheticBody : IrSyntheticBodyMessageType): IrStatementMessageType

    open fun readDescriptorReference(): DescriptorReferenceMessageType {
        var packageFqName: FqNameMessageType? = null
        var classFqName: FqNameMessageType? = null
        var name: Int? = null
        var uniqId: UniqIdMessageType? = null
        var isGetter: Boolean = false
        var isSetter: Boolean = false
        var isBackingField: Boolean = false
        var isFakeOverride: Boolean = false
        var isDefaultConstructor: Boolean = false
        var isEnumEntry: Boolean = false
        var isEnumSpecial: Boolean = false
        var isTypeParameter: Boolean = false
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> packageFqName = readWithLength { readFqName() }
                    2 -> classFqName = readWithLength { readFqName() }
                    3 -> name = readWithLength { readIrDataIndex() }
                    4 -> uniqId = readWithLength { readUniqId() }
                    5 -> isGetter = readBool()
                    6 -> isSetter = readBool()
                    7 -> isBackingField = readBool()
                    8 -> isFakeOverride = readBool()
                    9 -> isDefaultConstructor = readBool()
                    10 -> isEnumEntry = readBool()
                    11 -> isEnumSpecial = readBool()
                    12 -> isTypeParameter = readBool()
                    else -> skip(type)
                }
            }
        }
        return createDescriptorReference(packageFqName!!, classFqName!!, name!!, uniqId, isGetter, isSetter, isBackingField, isFakeOverride, isDefaultConstructor, isEnumEntry, isEnumSpecial, isTypeParameter)
    }

    open fun readUniqId(): UniqIdMessageType {
        var index: Long = 0L
        var isLocal: Boolean = false
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> index = readInt64()
                    2 -> isLocal = readBool()
                    else -> skip(type)
                }
            }
        }
        return createUniqId(index, isLocal)
    }

    open fun readVisibility(): VisibilityMessageType {
        var name: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> name = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createVisibility(name!!)
    }

    open fun readIrStatementOrigin(): IrStatementOriginMessageType {
        var name: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> name = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrStatementOrigin(name!!)
    }

    open fun readIrDeclarationOrigin(): IrDeclarationOriginMessageType {
        var oneOfOrigin: KnownOriginMessageType? = null
        var oneOfCustom: Int? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> {
                        oneOfOrigin = createKnownOrigin(readInt32())
                        oneOfIndex = 1
                    }
                    2 -> {
                        oneOfCustom = readWithLength { readIrDataIndex() }
                        oneOfIndex = 2
                    }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            1 -> return createIrDeclarationOrigin_origin(oneOfOrigin!!)
            2 -> return createIrDeclarationOrigin_custom(oneOfCustom!!)
            else -> error("Incorrect oneOf index: " + oneOfIndex)
        }
    }

    open fun readIrDataIndex(): Int {
        var index: Int = 0
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> index = readInt32()
                    else -> skip(type)
                }
            }
        }
        return createIrDataIndex(index)
    }

    open fun readFqName(): FqNameMessageType {
        var segment: MutableList<Int> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> segment.add(readWithLength { readIrDataIndex() })
                    else -> skip(type)
                }
            }
        }
        return createFqName(segment)
    }

    open fun readIrDeclarationContainer(): List<org.jetbrains.kotlin.ir.declarations.IrDeclaration> {
        var declaration: MutableList<IrDeclarationMessageType> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> declaration.add(readWithLength { readIrDeclaration() })
                    else -> skip(type)
                }
            }
        }
        return createIrDeclarationContainer(declaration)
    }

    open fun readFileEntry(): FileEntryMessageType {
        var name: String = ""
        var lineStartOffsets: MutableList<Int> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> name = readString()
                    2 -> lineStartOffsets.add(readInt32())
                    else -> skip(type)
                }
            }
        }
        return createFileEntry(name, lineStartOffsets)
    }

    open fun readIrFile(): IrFileMessageType {
        var declarationId: MutableList<UniqIdMessageType> = mutableListOf()
        var fileEntry: FileEntryMessageType? = null
        var fqName: FqNameMessageType? = null
        var annotations: List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>? = null
        var explicitlyExportedToCompiler: MutableList<Int> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> declarationId.add(readWithLength { readUniqId() })
                    2 -> fileEntry = readWithLength { readFileEntry() }
                    3 -> fqName = readWithLength { readFqName() }
                    4 -> annotations = readWithLength { readAnnotations() }
                    5 -> explicitlyExportedToCompiler.add(readWithLength { readIrDataIndex() })
                    else -> skip(type)
                }
            }
        }
        return createIrFile(declarationId, fileEntry!!, fqName!!, annotations!!, explicitlyExportedToCompiler)
    }

    open fun readStringTable(): Array<String> {
        var strings: MutableList<String> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> strings.add(readString())
                    else -> skip(type)
                }
            }
        }
        return createStringTable(strings)
    }

    open fun readIrSymbolData(): IrSymbolDataMessageType {
        var kind: Int? = null
        var uniqId: UniqIdMessageType? = null
        var topLevelUniqId: UniqIdMessageType? = null
        var fqname: FqNameMessageType? = null
        var descriptorReference: DescriptorReferenceMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> kind = createIrSymbolKind(readInt32())
                    2 -> uniqId = readWithLength { readUniqId() }
                    3 -> topLevelUniqId = readWithLength { readUniqId() }
                    4 -> fqname = readWithLength { readFqName() }
                    5 -> descriptorReference = readWithLength { readDescriptorReference() }
                    else -> skip(type)
                }
            }
        }
        return createIrSymbolData(kind!!, uniqId!!, topLevelUniqId!!, fqname, descriptorReference)
    }

    open fun readIrSymbolTable(): Array<org.jetbrains.kotlin.ir.symbols.IrSymbol> {
        var symbols: MutableList<IrSymbolDataMessageType> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbols.add(readWithLength { readIrSymbolData() })
                    else -> skip(type)
                }
            }
        }
        return createIrSymbolTable(symbols)
    }

    open fun readAnnotations(): List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall> {
        var annotation: MutableList<IrConstructorCallMessageType> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> annotation.add(readWithLength { readIrConstructorCall() })
                    else -> skip(type)
                }
            }
        }
        return createAnnotations(annotation)
    }

    open fun readTypeArguments(): List<org.jetbrains.kotlin.ir.types.IrType> {
        var typeArgument: MutableList<Int> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> typeArgument.add(readWithLength { readIrDataIndex() })
                    else -> skip(type)
                }
            }
        }
        return createTypeArguments(typeArgument)
    }

    open fun readIrStarProjection(): IrStarProjectionMessageType {
        var void: Boolean? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> void = readBool()
                    else -> skip(type)
                }
            }
        }
        return createIrStarProjection(void)
    }

    open fun readIrTypeProjection(): IrTypeProjectionMessageType {
        var variance: IrTypeVarianceMessageType? = null
        var type_: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> variance = createIrTypeVariance(readInt32())
                    2 -> type_ = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrTypeProjection(variance!!, type_!!)
    }

    open fun readIrTypeArgument(): IrTypeArgumentMessageType {
        var oneOfStar: IrStarProjectionMessageType? = null
        var oneOfType: IrTypeProjectionMessageType? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> {
                        oneOfStar = readWithLength { readIrStarProjection() }
                        oneOfIndex = 1
                    }
                    2 -> {
                        oneOfType = readWithLength { readIrTypeProjection() }
                        oneOfIndex = 2
                    }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            1 -> return createIrTypeArgument_star(oneOfStar!!)
            2 -> return createIrTypeArgument_type_(oneOfType!!)
            else -> error("Incorrect oneOf index: " + oneOfIndex)
        }
    }

    open fun readIrSimpleType(): IrSimpleTypeMessageType {
        var annotations: List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>? = null
        var classifier: Int? = null
        var hasQuestionMark: Boolean = false
        var argument: MutableList<IrTypeArgumentMessageType> = mutableListOf()
        var abbreviation: IrTypeAbbreviationMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> annotations = readWithLength { readAnnotations() }
                    2 -> classifier = readWithLength { readIrDataIndex() }
                    3 -> hasQuestionMark = readBool()
                    4 -> argument.add(readWithLength { readIrTypeArgument() })
                    5 -> abbreviation = readWithLength { readIrTypeAbbreviation() }
                    else -> skip(type)
                }
            }
        }
        return createIrSimpleType(annotations!!, classifier!!, hasQuestionMark, argument, abbreviation)
    }

    open fun readIrTypeAbbreviation(): IrTypeAbbreviationMessageType {
        var annotations: List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>? = null
        var typeAlias: Int? = null
        var hasQuestionMark: Boolean = false
        var argument: MutableList<IrTypeArgumentMessageType> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> annotations = readWithLength { readAnnotations() }
                    2 -> typeAlias = readWithLength { readIrDataIndex() }
                    3 -> hasQuestionMark = readBool()
                    4 -> argument.add(readWithLength { readIrTypeArgument() })
                    else -> skip(type)
                }
            }
        }
        return createIrTypeAbbreviation(annotations!!, typeAlias!!, hasQuestionMark, argument)
    }

    open fun readIrDynamicType(): IrDynamicTypeMessageType {
        var annotations: List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> annotations = readWithLength { readAnnotations() }
                    else -> skip(type)
                }
            }
        }
        return createIrDynamicType(annotations!!)
    }

    open fun readIrErrorType(): IrErrorTypeMessageType {
        var annotations: List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> annotations = readWithLength { readAnnotations() }
                    else -> skip(type)
                }
            }
        }
        return createIrErrorType(annotations!!)
    }

    open fun readIrType(): IrTypeMessageType {
        var oneOfSimple: IrSimpleTypeMessageType? = null
        var oneOfDynamic: IrDynamicTypeMessageType? = null
        var oneOfError: IrErrorTypeMessageType? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> {
                        oneOfSimple = readWithLength { readIrSimpleType() }
                        oneOfIndex = 1
                    }
                    2 -> {
                        oneOfDynamic = readWithLength { readIrDynamicType() }
                        oneOfIndex = 2
                    }
                    3 -> {
                        oneOfError = readWithLength { readIrErrorType() }
                        oneOfIndex = 3
                    }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            1 -> return createIrType_simple(oneOfSimple!!)
            2 -> return createIrType_dynamic(oneOfDynamic!!)
            3 -> return createIrType_error(oneOfError!!)
            else -> error("Incorrect oneOf index: " + oneOfIndex)
        }
    }

    open fun readIrTypeTable(): Array<org.jetbrains.kotlin.ir.types.IrType> {
        var types: MutableList<IrTypeMessageType> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> types.add(readWithLength { readIrType() })
                    else -> skip(type)
                }
            }
        }
        return createIrTypeTable(types)
    }

    open fun readIrBreak(): IrBreakMessageType {
        var loopId: Int = 0
        var label: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> loopId = readInt32()
                    2 -> label = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrBreak(loopId, label)
    }

    open fun readIrBlock(): IrBlockMessageType {
        var origin: IrStatementOriginMessageType? = null
        var statement: MutableList<IrStatementMessageType> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> origin = readWithLength { readIrStatementOrigin() }
                    2 -> statement.add(readWithLength { readIrStatement() })
                    else -> skip(type)
                }
            }
        }
        return createIrBlock(origin, statement)
    }

    open fun readIrCall(): IrCallMessageType {
        var symbol: Int? = null
        var memberAccessDispatchReceiver: IrExpressionMessageType? = null
        var memberAccessExtensionReceiver: IrExpressionMessageType? = null
        var memberAccessValueArgument: MutableList<NullableIrExpressionMessageType> = mutableListOf()
        var memberAccessTypeArguments: List<org.jetbrains.kotlin.ir.types.IrType>? = null
        var super_: Int? = null
        var origin: IrStatementOriginMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol = readWithLength { readIrDataIndex() }
                    2 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> memberAccessDispatchReceiver = readWithLength { readIrExpression() }
                                    2 -> memberAccessExtensionReceiver = readWithLength { readIrExpression() }
                                    3 -> memberAccessValueArgument.add(readWithLength { readNullableIrExpression() })
                                    4 -> memberAccessTypeArguments = readWithLength { readTypeArguments() }
                                    else -> skip(type)
                                }
                            }
                        }
                    }
                    3 -> super_ = readWithLength { readIrDataIndex() }
                    4 -> origin = readWithLength { readIrStatementOrigin() }
                    else -> skip(type)
                }
            }
        }
        return createIrCall(symbol!!, memberAccessDispatchReceiver, memberAccessExtensionReceiver, memberAccessValueArgument, memberAccessTypeArguments!!, super_, origin)
    }

    open fun readIrConstructorCall(): IrConstructorCallMessageType {
        var symbol: Int? = null
        var constructorTypeArgumentsCount: Int = 0
        var memberAccessDispatchReceiver: IrExpressionMessageType? = null
        var memberAccessExtensionReceiver: IrExpressionMessageType? = null
        var memberAccessValueArgument: MutableList<NullableIrExpressionMessageType> = mutableListOf()
        var memberAccessTypeArguments: List<org.jetbrains.kotlin.ir.types.IrType>? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol = readWithLength { readIrDataIndex() }
                    2 -> constructorTypeArgumentsCount = readInt32()
                    3 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> memberAccessDispatchReceiver = readWithLength { readIrExpression() }
                                    2 -> memberAccessExtensionReceiver = readWithLength { readIrExpression() }
                                    3 -> memberAccessValueArgument.add(readWithLength { readNullableIrExpression() })
                                    4 -> memberAccessTypeArguments = readWithLength { readTypeArguments() }
                                    else -> skip(type)
                                }
                            }
                        }
                    }
                    else -> skip(type)
                }
            }
        }
        return createIrConstructorCall(symbol!!, constructorTypeArgumentsCount, memberAccessDispatchReceiver, memberAccessExtensionReceiver, memberAccessValueArgument, memberAccessTypeArguments!!)
    }

    open fun readIrFunctionReference(): IrFunctionReferenceMessageType {
        var symbol: Int? = null
        var origin: IrStatementOriginMessageType? = null
        var memberAccessDispatchReceiver: IrExpressionMessageType? = null
        var memberAccessExtensionReceiver: IrExpressionMessageType? = null
        var memberAccessValueArgument: MutableList<NullableIrExpressionMessageType> = mutableListOf()
        var memberAccessTypeArguments: List<org.jetbrains.kotlin.ir.types.IrType>? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol = readWithLength { readIrDataIndex() }
                    2 -> origin = readWithLength { readIrStatementOrigin() }
                    3 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> memberAccessDispatchReceiver = readWithLength { readIrExpression() }
                                    2 -> memberAccessExtensionReceiver = readWithLength { readIrExpression() }
                                    3 -> memberAccessValueArgument.add(readWithLength { readNullableIrExpression() })
                                    4 -> memberAccessTypeArguments = readWithLength { readTypeArguments() }
                                    else -> skip(type)
                                }
                            }
                        }
                    }
                    else -> skip(type)
                }
            }
        }
        return createIrFunctionReference(symbol!!, origin, memberAccessDispatchReceiver, memberAccessExtensionReceiver, memberAccessValueArgument, memberAccessTypeArguments!!)
    }

    open fun readIrLocalDelegatedPropertyReference(): IrLocalDelegatedPropertyReferenceMessageType {
        var delegate: Int? = null
        var getter: Int? = null
        var setter: Int? = null
        var symbol: Int? = null
        var origin: IrStatementOriginMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> delegate = readWithLength { readIrDataIndex() }
                    2 -> getter = readWithLength { readIrDataIndex() }
                    3 -> setter = readWithLength { readIrDataIndex() }
                    4 -> symbol = readWithLength { readIrDataIndex() }
                    5 -> origin = readWithLength { readIrStatementOrigin() }
                    else -> skip(type)
                }
            }
        }
        return createIrLocalDelegatedPropertyReference(delegate!!, getter, setter, symbol!!, origin)
    }

    open fun readIrPropertyReference(): IrPropertyReferenceMessageType {
        var field: Int? = null
        var getter: Int? = null
        var setter: Int? = null
        var origin: IrStatementOriginMessageType? = null
        var memberAccessDispatchReceiver: IrExpressionMessageType? = null
        var memberAccessExtensionReceiver: IrExpressionMessageType? = null
        var memberAccessValueArgument: MutableList<NullableIrExpressionMessageType> = mutableListOf()
        var memberAccessTypeArguments: List<org.jetbrains.kotlin.ir.types.IrType>? = null
        var symbol: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> field = readWithLength { readIrDataIndex() }
                    2 -> getter = readWithLength { readIrDataIndex() }
                    3 -> setter = readWithLength { readIrDataIndex() }
                    4 -> origin = readWithLength { readIrStatementOrigin() }
                    5 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> memberAccessDispatchReceiver = readWithLength { readIrExpression() }
                                    2 -> memberAccessExtensionReceiver = readWithLength { readIrExpression() }
                                    3 -> memberAccessValueArgument.add(readWithLength { readNullableIrExpression() })
                                    4 -> memberAccessTypeArguments = readWithLength { readTypeArguments() }
                                    else -> skip(type)
                                }
                            }
                        }
                    }
                    6 -> symbol = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrPropertyReference(field, getter, setter, origin, memberAccessDispatchReceiver, memberAccessExtensionReceiver, memberAccessValueArgument, memberAccessTypeArguments!!, symbol!!)
    }

    open fun readIrComposite(): IrCompositeMessageType {
        var statement: MutableList<IrStatementMessageType> = mutableListOf()
        var origin: IrStatementOriginMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> statement.add(readWithLength { readIrStatement() })
                    2 -> origin = readWithLength { readIrStatementOrigin() }
                    else -> skip(type)
                }
            }
        }
        return createIrComposite(statement, origin)
    }

    open fun readIrClassReference(): IrClassReferenceMessageType {
        var classSymbol: Int? = null
        var classType: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> classSymbol = readWithLength { readIrDataIndex() }
                    2 -> classType = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrClassReference(classSymbol!!, classType!!)
    }

    open fun readIrConst(): IrConstMessageType<*> {
        var oneOfNull: Boolean? = null
        var oneOfBoolean: Boolean? = null
        var oneOfChar: Int? = null
        var oneOfByte: Int? = null
        var oneOfShort: Int? = null
        var oneOfInt: Int? = null
        var oneOfLong: Long? = null
        var oneOfFloat: Float? = null
        var oneOfDouble: Double? = null
        var oneOfString: Int? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> {
                        oneOfNull = readBool()
                        oneOfIndex = 1
                    }
                    2 -> {
                        oneOfBoolean = readBool()
                        oneOfIndex = 2
                    }
                    3 -> {
                        oneOfChar = readInt32()
                        oneOfIndex = 3
                    }
                    4 -> {
                        oneOfByte = readInt32()
                        oneOfIndex = 4
                    }
                    5 -> {
                        oneOfShort = readInt32()
                        oneOfIndex = 5
                    }
                    6 -> {
                        oneOfInt = readInt32()
                        oneOfIndex = 6
                    }
                    7 -> {
                        oneOfLong = readInt64()
                        oneOfIndex = 7
                    }
                    8 -> {
                        oneOfFloat = readFloat()
                        oneOfIndex = 8
                    }
                    9 -> {
                        oneOfDouble = readDouble()
                        oneOfIndex = 9
                    }
                    10 -> {
                        oneOfString = readWithLength { readIrDataIndex() }
                        oneOfIndex = 10
                    }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            1 -> return createIrConst_null_(oneOfNull!!)
            2 -> return createIrConst_boolean(oneOfBoolean!!)
            3 -> return createIrConst_char(oneOfChar!!)
            4 -> return createIrConst_byte(oneOfByte!!)
            5 -> return createIrConst_short(oneOfShort!!)
            6 -> return createIrConst_int(oneOfInt!!)
            7 -> return createIrConst_long(oneOfLong!!)
            8 -> return createIrConst_float(oneOfFloat!!)
            9 -> return createIrConst_double(oneOfDouble!!)
            10 -> return createIrConst_string(oneOfString!!)
            else -> error("Incorrect oneOf index: " + oneOfIndex)
        }
    }

    open fun readIrContinue(): IrContinueMessageType {
        var loopId: Int = 0
        var label: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> loopId = readInt32()
                    2 -> label = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrContinue(loopId, label)
    }

    open fun readIrDelegatingConstructorCall(): IrDelegatingConstructorCallMessageType {
        var symbol: Int? = null
        var memberAccessDispatchReceiver: IrExpressionMessageType? = null
        var memberAccessExtensionReceiver: IrExpressionMessageType? = null
        var memberAccessValueArgument: MutableList<NullableIrExpressionMessageType> = mutableListOf()
        var memberAccessTypeArguments: List<org.jetbrains.kotlin.ir.types.IrType>? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol = readWithLength { readIrDataIndex() }
                    2 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> memberAccessDispatchReceiver = readWithLength { readIrExpression() }
                                    2 -> memberAccessExtensionReceiver = readWithLength { readIrExpression() }
                                    3 -> memberAccessValueArgument.add(readWithLength { readNullableIrExpression() })
                                    4 -> memberAccessTypeArguments = readWithLength { readTypeArguments() }
                                    else -> skip(type)
                                }
                            }
                        }
                    }
                    else -> skip(type)
                }
            }
        }
        return createIrDelegatingConstructorCall(symbol!!, memberAccessDispatchReceiver, memberAccessExtensionReceiver, memberAccessValueArgument, memberAccessTypeArguments!!)
    }

    open fun readIrDoWhile(): IrDoWhileMessageType {
        var loopLoopId: Int = 0
        var loopCondition: IrExpressionMessageType? = null
        var loopLabel: Int? = null
        var loopBody: IrExpressionMessageType? = null
        var loopBodyOffset: Int = -1
        var loopOrigin: IrStatementOriginMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> loopLoopId = readInt32()
                                    2 -> loopCondition = readWithLength { readIrExpression() }
                                    3 -> loopLabel = readWithLength { readIrDataIndex() }
                                    4 -> {
                                        loopBodyOffset = offset
                                        skip(type)
                                    }
                                    5 -> loopOrigin = readWithLength { readIrStatementOrigin() }
                                    else -> skip(type)
                                }
                            }
                        }
                    }
                    else -> skip(type)
                }
            }
        }
        val p0 = createIrDoWhile(loopLoopId, loopCondition!!, loopLabel, loopOrigin)

        if (loopBodyOffset != -1) {
            loopBody = delayed(loopBodyOffset) { readWithLength { readIrExpression() } }
        }
        return createIrDoWhile1(p0, loopBody)
    }

    open fun readIrEnumConstructorCall(): IrEnumConstructorCallMessageType {
        var symbol: Int? = null
        var memberAccessDispatchReceiver: IrExpressionMessageType? = null
        var memberAccessExtensionReceiver: IrExpressionMessageType? = null
        var memberAccessValueArgument: MutableList<NullableIrExpressionMessageType> = mutableListOf()
        var memberAccessTypeArguments: List<org.jetbrains.kotlin.ir.types.IrType>? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol = readWithLength { readIrDataIndex() }
                    2 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> memberAccessDispatchReceiver = readWithLength { readIrExpression() }
                                    2 -> memberAccessExtensionReceiver = readWithLength { readIrExpression() }
                                    3 -> memberAccessValueArgument.add(readWithLength { readNullableIrExpression() })
                                    4 -> memberAccessTypeArguments = readWithLength { readTypeArguments() }
                                    else -> skip(type)
                                }
                            }
                        }
                    }
                    else -> skip(type)
                }
            }
        }
        return createIrEnumConstructorCall(symbol!!, memberAccessDispatchReceiver, memberAccessExtensionReceiver, memberAccessValueArgument, memberAccessTypeArguments!!)
    }

    open fun readIrGetClass(): IrGetClassMessageType {
        var argument: IrExpressionMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> argument = readWithLength { readIrExpression() }
                    else -> skip(type)
                }
            }
        }
        return createIrGetClass(argument!!)
    }

    open fun readIrGetEnumValue(): IrGetEnumValueMessageType {
        var symbol: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    2 -> symbol = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrGetEnumValue(symbol!!)
    }

    open fun readIrGetField(): IrGetFieldMessageType {
        var fieldAccessSymbol: Int? = null
        var fieldAccessSuper: Int? = null
        var fieldAccessReceiver: IrExpressionMessageType? = null
        var origin: IrStatementOriginMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> fieldAccessSymbol = readWithLength { readIrDataIndex() }
                                    2 -> fieldAccessSuper = readWithLength { readIrDataIndex() }
                                    3 -> fieldAccessReceiver = readWithLength { readIrExpression() }
                                    else -> skip(type)
                                }
                            }
                        }
                    }
                    2 -> origin = readWithLength { readIrStatementOrigin() }
                    else -> skip(type)
                }
            }
        }
        return createIrGetField(fieldAccessSymbol!!, fieldAccessSuper, fieldAccessReceiver, origin)
    }

    open fun readIrGetValue(): IrGetValueMessageType {
        var symbol: Int? = null
        var origin: IrStatementOriginMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol = readWithLength { readIrDataIndex() }
                    2 -> origin = readWithLength { readIrStatementOrigin() }
                    else -> skip(type)
                }
            }
        }
        return createIrGetValue(symbol!!, origin)
    }

    open fun readIrGetObject(): IrGetObjectMessageType {
        var symbol: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrGetObject(symbol!!)
    }

    open fun readIrInstanceInitializerCall(): IrInstanceInitializerCallMessageType {
        var symbol: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrInstanceInitializerCall(symbol!!)
    }

    open fun readIrReturn(): IrReturnMessageType {
        var returnTarget: Int? = null
        var value: IrExpressionMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> returnTarget = readWithLength { readIrDataIndex() }
                    2 -> value = readWithLength { readIrExpression() }
                    else -> skip(type)
                }
            }
        }
        return createIrReturn(returnTarget!!, value!!)
    }

    open fun readIrSetField(): IrSetFieldMessageType {
        var fieldAccessSymbol: Int? = null
        var fieldAccessSuper: Int? = null
        var fieldAccessReceiver: IrExpressionMessageType? = null
        var value: IrExpressionMessageType? = null
        var origin: IrStatementOriginMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> fieldAccessSymbol = readWithLength { readIrDataIndex() }
                                    2 -> fieldAccessSuper = readWithLength { readIrDataIndex() }
                                    3 -> fieldAccessReceiver = readWithLength { readIrExpression() }
                                    else -> skip(type)
                                }
                            }
                        }
                    }
                    2 -> value = readWithLength { readIrExpression() }
                    3 -> origin = readWithLength { readIrStatementOrigin() }
                    else -> skip(type)
                }
            }
        }
        return createIrSetField(fieldAccessSymbol!!, fieldAccessSuper, fieldAccessReceiver, value!!, origin)
    }

    open fun readIrSetVariable(): IrSetVariableMessageType {
        var symbol: Int? = null
        var value: IrExpressionMessageType? = null
        var origin: IrStatementOriginMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol = readWithLength { readIrDataIndex() }
                    2 -> value = readWithLength { readIrExpression() }
                    3 -> origin = readWithLength { readIrStatementOrigin() }
                    else -> skip(type)
                }
            }
        }
        return createIrSetVariable(symbol!!, value!!, origin)
    }

    open fun readIrSpreadElement(): IrSpreadElementMessageType {
        var expression: IrExpressionMessageType? = null
        var coordinatesStartOffset: Int = 0
        var coordinatesEndOffset: Int = 0
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> expression = readWithLength { readIrExpression() }
                    2 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> coordinatesStartOffset = readInt32()
                                    2 -> coordinatesEndOffset = readInt32()
                                    else -> skip(type)
                                }
                            }
                        }
                    }
                    else -> skip(type)
                }
            }
        }
        return createIrSpreadElement(expression!!, coordinatesStartOffset, coordinatesEndOffset)
    }

    open fun readIrStringConcat(): IrStringConcatMessageType {
        var argument: MutableList<IrExpressionMessageType> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> argument.add(readWithLength { readIrExpression() })
                    else -> skip(type)
                }
            }
        }
        return createIrStringConcat(argument)
    }

    open fun readIrThrow(): IrThrowMessageType {
        var value: IrExpressionMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> value = readWithLength { readIrExpression() }
                    else -> skip(type)
                }
            }
        }
        return createIrThrow(value!!)
    }

    open fun readIrTry(): IrTryMessageType {
        var result: IrExpressionMessageType? = null
        var catch: MutableList<IrStatementMessageType> = mutableListOf()
        var finally: IrExpressionMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> result = readWithLength { readIrExpression() }
                    2 -> catch.add(readWithLength { readIrStatement() })
                    3 -> finally = readWithLength { readIrExpression() }
                    else -> skip(type)
                }
            }
        }
        return createIrTry(result!!, catch, finally)
    }

    open fun readIrTypeOp(): IrTypeOpMessageType {
        var operator: IrTypeOperatorMessageType? = null
        var operand: Int? = null
        var argument: IrExpressionMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> operator = createIrTypeOperator(readInt32())
                    2 -> operand = readWithLength { readIrDataIndex() }
                    3 -> argument = readWithLength { readIrExpression() }
                    else -> skip(type)
                }
            }
        }
        return createIrTypeOp(operator!!, operand!!, argument!!)
    }

    open fun readIrVararg(): IrVarargMessageType {
        var elementType: Int? = null
        var element: MutableList<IrVarargElementMessageType> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> elementType = readWithLength { readIrDataIndex() }
                    2 -> element.add(readWithLength { readIrVarargElement() })
                    else -> skip(type)
                }
            }
        }
        return createIrVararg(elementType!!, element)
    }

    open fun readIrVarargElement(): IrVarargElementMessageType {
        var oneOfExpression: IrExpressionMessageType? = null
        var oneOfSpreadElement: IrSpreadElementMessageType? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> {
                        oneOfExpression = readWithLength { readIrExpression() }
                        oneOfIndex = 1
                    }
                    2 -> {
                        oneOfSpreadElement = readWithLength { readIrSpreadElement() }
                        oneOfIndex = 2
                    }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            1 -> return createIrVarargElement_expression(oneOfExpression!!)
            2 -> return createIrVarargElement_spreadElement(oneOfSpreadElement!!)
            else -> error("Incorrect oneOf index: " + oneOfIndex)
        }
    }

    open fun readIrWhen(): IrWhenMessageType {
        var branch: MutableList<IrStatementMessageType> = mutableListOf()
        var origin: IrStatementOriginMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> branch.add(readWithLength { readIrStatement() })
                    2 -> origin = readWithLength { readIrStatementOrigin() }
                    else -> skip(type)
                }
            }
        }
        return createIrWhen(branch, origin)
    }

    open fun readIrWhile(): IrWhileMessageType {
        var loopLoopId: Int = 0
        var loopCondition: IrExpressionMessageType? = null
        var loopLabel: Int? = null
        var loopBody: IrExpressionMessageType? = null
        var loopBodyOffset: Int = -1
        var loopOrigin: IrStatementOriginMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> loopLoopId = readInt32()
                                    2 -> loopCondition = readWithLength { readIrExpression() }
                                    3 -> loopLabel = readWithLength { readIrDataIndex() }
                                    4 -> {
                                        loopBodyOffset = offset
                                        skip(type)
                                    }
                                    5 -> loopOrigin = readWithLength { readIrStatementOrigin() }
                                    else -> skip(type)
                                }
                            }
                        }
                    }
                    else -> skip(type)
                }
            }
        }
        val p0 = createIrWhile(loopLoopId, loopCondition!!, loopLabel, loopOrigin)

        if (loopBodyOffset != -1) {
            loopBody = delayed(loopBodyOffset) { readWithLength { readIrExpression() } }
        }
        return createIrWhile1(p0, loopBody)
    }

    open fun readIrFunctionExpression(): IrFunctionExpressionMessageType {
        var function: IrFunctionMessageType? = null
        var origin: IrStatementOriginMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> function = readWithLength { readIrFunction() }
                    2 -> origin = readWithLength { readIrStatementOrigin() }
                    else -> skip(type)
                }
            }
        }
        return createIrFunctionExpression(function!!, origin!!)
    }

    open fun readIrDynamicMemberExpression(): IrDynamicMemberExpressionMessageType {
        var memberName: Int? = null
        var receiver: IrExpressionMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> memberName = readWithLength { readIrDataIndex() }
                    2 -> receiver = readWithLength { readIrExpression() }
                    else -> skip(type)
                }
            }
        }
        return createIrDynamicMemberExpression(memberName!!, receiver!!)
    }

    open fun readIrDynamicOperatorExpression(): IrDynamicOperatorExpressionMessageType {
        var operator: IrDynamicOperatorMessageType? = null
        var receiver: IrExpressionMessageType? = null
        var argument: MutableList<IrExpressionMessageType> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> operator = createIrDynamicOperator(readInt32())
                    2 -> receiver = readWithLength { readIrExpression() }
                    3 -> argument.add(readWithLength { readIrExpression() })
                    else -> skip(type)
                }
            }
        }
        return createIrDynamicOperatorExpression(operator!!, receiver!!, argument)
    }

    open fun readIrOperation(): IrOperationMessageType {
        var oneOfBlock: IrBlockMessageType? = null
        var oneOfBreak: IrBreakMessageType? = null
        var oneOfCall: IrCallMessageType? = null
        var oneOfClassReference: IrClassReferenceMessageType? = null
        var oneOfComposite: IrCompositeMessageType? = null
        var oneOfConst: IrConstMessageType<*>? = null
        var oneOfContinue: IrContinueMessageType? = null
        var oneOfDelegatingConstructorCall: IrDelegatingConstructorCallMessageType? = null
        var oneOfDoWhile: IrDoWhileMessageType? = null
        var oneOfEnumConstructorCall: IrEnumConstructorCallMessageType? = null
        var oneOfFunctionReference: IrFunctionReferenceMessageType? = null
        var oneOfGetClass: IrGetClassMessageType? = null
        var oneOfGetEnumValue: IrGetEnumValueMessageType? = null
        var oneOfGetField: IrGetFieldMessageType? = null
        var oneOfGetObject: IrGetObjectMessageType? = null
        var oneOfGetValue: IrGetValueMessageType? = null
        var oneOfInstanceInitializerCall: IrInstanceInitializerCallMessageType? = null
        var oneOfPropertyReference: IrPropertyReferenceMessageType? = null
        var oneOfReturn: IrReturnMessageType? = null
        var oneOfSetField: IrSetFieldMessageType? = null
        var oneOfSetVariable: IrSetVariableMessageType? = null
        var oneOfStringConcat: IrStringConcatMessageType? = null
        var oneOfThrow: IrThrowMessageType? = null
        var oneOfTry: IrTryMessageType? = null
        var oneOfTypeOp: IrTypeOpMessageType? = null
        var oneOfVararg: IrVarargMessageType? = null
        var oneOfWhen: IrWhenMessageType? = null
        var oneOfWhile: IrWhileMessageType? = null
        var oneOfDynamicMember: IrDynamicMemberExpressionMessageType? = null
        var oneOfDynamicOperator: IrDynamicOperatorExpressionMessageType? = null
        var oneOfLocalDelegatedPropertyReference: IrLocalDelegatedPropertyReferenceMessageType? = null
        var oneOfConstructorCall: IrConstructorCallMessageType? = null
        var oneOfFunctionExpression: IrFunctionExpressionMessageType? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> {
                        oneOfBlock = readWithLength { readIrBlock() }
                        oneOfIndex = 1
                    }
                    2 -> {
                        oneOfBreak = readWithLength { readIrBreak() }
                        oneOfIndex = 2
                    }
                    3 -> {
                        oneOfCall = readWithLength { readIrCall() }
                        oneOfIndex = 3
                    }
                    4 -> {
                        oneOfClassReference = readWithLength { readIrClassReference() }
                        oneOfIndex = 4
                    }
                    5 -> {
                        oneOfComposite = readWithLength { readIrComposite() }
                        oneOfIndex = 5
                    }
                    6 -> {
                        oneOfConst = readWithLength { readIrConst() }
                        oneOfIndex = 6
                    }
                    7 -> {
                        oneOfContinue = readWithLength { readIrContinue() }
                        oneOfIndex = 7
                    }
                    8 -> {
                        oneOfDelegatingConstructorCall = readWithLength { readIrDelegatingConstructorCall() }
                        oneOfIndex = 8
                    }
                    9 -> {
                        oneOfDoWhile = readWithLength { readIrDoWhile() }
                        oneOfIndex = 9
                    }
                    10 -> {
                        oneOfEnumConstructorCall = readWithLength { readIrEnumConstructorCall() }
                        oneOfIndex = 10
                    }
                    11 -> {
                        oneOfFunctionReference = readWithLength { readIrFunctionReference() }
                        oneOfIndex = 11
                    }
                    12 -> {
                        oneOfGetClass = readWithLength { readIrGetClass() }
                        oneOfIndex = 12
                    }
                    13 -> {
                        oneOfGetEnumValue = readWithLength { readIrGetEnumValue() }
                        oneOfIndex = 13
                    }
                    14 -> {
                        oneOfGetField = readWithLength { readIrGetField() }
                        oneOfIndex = 14
                    }
                    15 -> {
                        oneOfGetObject = readWithLength { readIrGetObject() }
                        oneOfIndex = 15
                    }
                    16 -> {
                        oneOfGetValue = readWithLength { readIrGetValue() }
                        oneOfIndex = 16
                    }
                    17 -> {
                        oneOfInstanceInitializerCall = readWithLength { readIrInstanceInitializerCall() }
                        oneOfIndex = 17
                    }
                    18 -> {
                        oneOfPropertyReference = readWithLength { readIrPropertyReference() }
                        oneOfIndex = 18
                    }
                    19 -> {
                        oneOfReturn = readWithLength { readIrReturn() }
                        oneOfIndex = 19
                    }
                    20 -> {
                        oneOfSetField = readWithLength { readIrSetField() }
                        oneOfIndex = 20
                    }
                    21 -> {
                        oneOfSetVariable = readWithLength { readIrSetVariable() }
                        oneOfIndex = 21
                    }
                    22 -> {
                        oneOfStringConcat = readWithLength { readIrStringConcat() }
                        oneOfIndex = 22
                    }
                    23 -> {
                        oneOfThrow = readWithLength { readIrThrow() }
                        oneOfIndex = 23
                    }
                    24 -> {
                        oneOfTry = readWithLength { readIrTry() }
                        oneOfIndex = 24
                    }
                    25 -> {
                        oneOfTypeOp = readWithLength { readIrTypeOp() }
                        oneOfIndex = 25
                    }
                    26 -> {
                        oneOfVararg = readWithLength { readIrVararg() }
                        oneOfIndex = 26
                    }
                    27 -> {
                        oneOfWhen = readWithLength { readIrWhen() }
                        oneOfIndex = 27
                    }
                    28 -> {
                        oneOfWhile = readWithLength { readIrWhile() }
                        oneOfIndex = 28
                    }
                    29 -> {
                        oneOfDynamicMember = readWithLength { readIrDynamicMemberExpression() }
                        oneOfIndex = 29
                    }
                    30 -> {
                        oneOfDynamicOperator = readWithLength { readIrDynamicOperatorExpression() }
                        oneOfIndex = 30
                    }
                    31 -> {
                        oneOfLocalDelegatedPropertyReference = readWithLength { readIrLocalDelegatedPropertyReference() }
                        oneOfIndex = 31
                    }
                    32 -> {
                        oneOfConstructorCall = readWithLength { readIrConstructorCall() }
                        oneOfIndex = 32
                    }
                    33 -> {
                        oneOfFunctionExpression = readWithLength { readIrFunctionExpression() }
                        oneOfIndex = 33
                    }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            1 -> return createIrOperation_block(oneOfBlock!!)
            2 -> return createIrOperation_break_(oneOfBreak!!)
            3 -> return createIrOperation_call(oneOfCall!!)
            4 -> return createIrOperation_classReference(oneOfClassReference!!)
            5 -> return createIrOperation_composite(oneOfComposite!!)
            6 -> return createIrOperation_const(oneOfConst!!)
            7 -> return createIrOperation_continue_(oneOfContinue!!)
            8 -> return createIrOperation_delegatingConstructorCall(oneOfDelegatingConstructorCall!!)
            9 -> return createIrOperation_doWhile(oneOfDoWhile!!)
            10 -> return createIrOperation_enumConstructorCall(oneOfEnumConstructorCall!!)
            11 -> return createIrOperation_functionReference(oneOfFunctionReference!!)
            12 -> return createIrOperation_getClass(oneOfGetClass!!)
            13 -> return createIrOperation_getEnumValue(oneOfGetEnumValue!!)
            14 -> return createIrOperation_getField(oneOfGetField!!)
            15 -> return createIrOperation_getObject(oneOfGetObject!!)
            16 -> return createIrOperation_getValue(oneOfGetValue!!)
            17 -> return createIrOperation_instanceInitializerCall(oneOfInstanceInitializerCall!!)
            18 -> return createIrOperation_propertyReference(oneOfPropertyReference!!)
            19 -> return createIrOperation_return_(oneOfReturn!!)
            20 -> return createIrOperation_setField(oneOfSetField!!)
            21 -> return createIrOperation_setVariable(oneOfSetVariable!!)
            22 -> return createIrOperation_stringConcat(oneOfStringConcat!!)
            23 -> return createIrOperation_throw_(oneOfThrow!!)
            24 -> return createIrOperation_try_(oneOfTry!!)
            25 -> return createIrOperation_typeOp(oneOfTypeOp!!)
            26 -> return createIrOperation_vararg(oneOfVararg!!)
            27 -> return createIrOperation_when_(oneOfWhen!!)
            28 -> return createIrOperation_while_(oneOfWhile!!)
            29 -> return createIrOperation_dynamicMember(oneOfDynamicMember!!)
            30 -> return createIrOperation_dynamicOperator(oneOfDynamicOperator!!)
            31 -> return createIrOperation_localDelegatedPropertyReference(oneOfLocalDelegatedPropertyReference!!)
            32 -> return createIrOperation_constructorCall(oneOfConstructorCall!!)
            33 -> return createIrOperation_functionExpression(oneOfFunctionExpression!!)
            else -> error("Incorrect oneOf index: " + oneOfIndex)
        }
    }

    protected var fieldIrExpressionType: Int? = null
    protected var fieldIrExpressionCoordinatesStartOffset: Int = 0
    protected var fieldIrExpressionCoordinatesEndOffset: Int = 0

    open fun readIrExpression(): IrExpressionMessageType {
        var operation: IrOperationMessageType? = null
        var operationOffset: Int = -1
        var type_: Int? = null
        var coordinatesStartOffset: Int = 0
        var coordinatesEndOffset: Int = 0
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> {
                        operationOffset = offset
                        skip(type)
                    }
                    2 -> type_ = readWithLength { readIrDataIndex() }
                    3 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> coordinatesStartOffset = readInt32()
                                    2 -> coordinatesEndOffset = readInt32()
                                    else -> skip(type)
                                }
                            }
                        }
                    }
                    else -> skip(type)
                }
            }
        }
        val oldfieldIrExpressionType = fieldIrExpressionType
        fieldIrExpressionType = type_
        val oldfieldIrExpressionCoordinatesStartOffset = fieldIrExpressionCoordinatesStartOffset
        fieldIrExpressionCoordinatesStartOffset = coordinatesStartOffset
        val oldfieldIrExpressionCoordinatesEndOffset = fieldIrExpressionCoordinatesEndOffset
        fieldIrExpressionCoordinatesEndOffset = coordinatesEndOffset
        if (operationOffset != -1) {
            operation = delayed(operationOffset) { readWithLength { readIrOperation() } }
        }
        val p0 = createIrExpression(operation!!, type_!!, coordinatesStartOffset, coordinatesEndOffset)
        fieldIrExpressionType = oldfieldIrExpressionType
        fieldIrExpressionCoordinatesStartOffset = oldfieldIrExpressionCoordinatesStartOffset
        fieldIrExpressionCoordinatesEndOffset = oldfieldIrExpressionCoordinatesEndOffset
        return p0
    }

    open fun readNullableIrExpression(): NullableIrExpressionMessageType {
        var expression: IrExpressionMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> expression = readWithLength { readIrExpression() }
                    else -> skip(type)
                }
            }
        }
        return createNullableIrExpression(expression)
    }

    open fun readIrFunction(): IrFunctionMessageType {
        var baseBaseSymbol: Int? = null
        var baseBaseOrigin: IrDeclarationOriginMessageType? = null
        var baseBaseCoordinatesStartOffset: Int = 0
        var baseBaseCoordinatesEndOffset: Int = 0
        var baseBaseAnnotations: List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>? = null
        var baseName: Int? = null
        var baseVisibility: VisibilityMessageType? = null
        var baseIsInline: Boolean = false
        var baseIsExternal: Boolean = false
        var baseTypeParameters: List<org.jetbrains.kotlin.ir.declarations.IrTypeParameter>? = null
        var baseTypeParametersOffset: Int = -1
        var baseDispatchReceiver: IrValueParameterMessageType? = null
        var baseDispatchReceiverOffset: Int = -1
        var baseExtensionReceiver: IrValueParameterMessageType? = null
        var baseExtensionReceiverOffset: Int = -1
        var baseValueParameter: MutableList<IrValueParameterMessageType> = mutableListOf()
        var baseValueParameterOffsetList: MutableList<Int> = arrayListOf()
        var baseBody: Int? = null
        var baseBodyOffset: Int = -1
        var baseReturnType: Int? = null
        var modality: ModalityKindMessageType? = null
        var isTailrec: Boolean = false
        var isSuspend: Boolean = false
        var overridden: MutableList<Int> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> readWithLength {
                                        while (hasData) {
                                            readField { fieldNumber, type ->
                                                when (fieldNumber) {
                                                    1 -> baseBaseSymbol = readWithLength { readIrDataIndex() }
                                                    2 -> baseBaseOrigin = readWithLength { readIrDeclarationOrigin() }
                                                    3 -> readWithLength {
                                                        while (hasData) {
                                                            readField { fieldNumber, type ->
                                                                when (fieldNumber) {
                                                                    1 -> baseBaseCoordinatesStartOffset = readInt32()
                                                                    2 -> baseBaseCoordinatesEndOffset = readInt32()
                                                                    else -> skip(type)
                                                                }
                                                            }
                                                        }
                                                    }
                                                    4 -> baseBaseAnnotations = readWithLength { readAnnotations() }
                                                    else -> skip(type)
                                                }
                                            }
                                        }
                                    }
                                    2 -> baseName = readWithLength { readIrDataIndex() }
                                    3 -> baseVisibility = readWithLength { readVisibility() }
                                    4 -> baseIsInline = readBool()
                                    5 -> baseIsExternal = readBool()
                                    6 -> {
                                        baseTypeParametersOffset = offset
                                        skip(type)
                                    }
                                    7 -> {
                                        baseDispatchReceiverOffset = offset
                                        skip(type)
                                    }
                                    8 -> {
                                        baseExtensionReceiverOffset = offset
                                        skip(type)
                                    }
                                    9 -> {
                                        baseValueParameterOffsetList.add(offset)
                                        skip(type)
                                    }
                                    10 -> {
                                        baseBodyOffset = offset
                                        skip(type)
                                    }
                                    11 -> baseReturnType = readWithLength { readIrDataIndex() }
                                    else -> skip(type)
                                }
                            }
                        }
                    }
                    2 -> modality = createModalityKind(readInt32())
                    3 -> isTailrec = readBool()
                    4 -> isSuspend = readBool()
                    5 -> overridden.add(readWithLength { readIrDataIndex() })
                    else -> skip(type)
                }
            }
        }
        val p0 = createIrFunction(baseBaseSymbol!!, baseBaseOrigin!!, baseBaseCoordinatesStartOffset, baseBaseCoordinatesEndOffset, baseBaseAnnotations!!, baseName!!, baseVisibility!!, baseIsInline, baseIsExternal, baseReturnType!!, modality!!, isTailrec, isSuspend, overridden)

        if (baseTypeParametersOffset != -1) {
            baseTypeParameters = delayed(baseTypeParametersOffset) { readWithLength { readIrTypeParameterContainer() } }
        }
        if (baseDispatchReceiverOffset != -1) {
            baseDispatchReceiver = delayed(baseDispatchReceiverOffset) { readWithLength { readIrValueParameter() } }
        }
        if (baseExtensionReceiverOffset != -1) {
            baseExtensionReceiver = delayed(baseExtensionReceiverOffset) { readWithLength { readIrValueParameter() } }
        }
        for (o in baseValueParameterOffsetList) {
            baseValueParameter.add(delayed(o) { readWithLength { readIrValueParameter() } })
        }
        if (baseBodyOffset != -1) {
            baseBody = delayed(baseBodyOffset) { readWithLength { readIrDataIndex() } }
        }
        return createIrFunction1(p0, baseTypeParameters!!, baseDispatchReceiver, baseExtensionReceiver, baseValueParameter, baseBody)
    }

    open fun readIrConstructor(): IrConstructorMessageType {
        var baseBaseSymbol: Int? = null
        var baseBaseOrigin: IrDeclarationOriginMessageType? = null
        var baseBaseCoordinatesStartOffset: Int = 0
        var baseBaseCoordinatesEndOffset: Int = 0
        var baseBaseAnnotations: List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>? = null
        var baseName: Int? = null
        var baseVisibility: VisibilityMessageType? = null
        var baseIsInline: Boolean = false
        var baseIsExternal: Boolean = false
        var baseTypeParameters: List<org.jetbrains.kotlin.ir.declarations.IrTypeParameter>? = null
        var baseTypeParametersOffset: Int = -1
        var baseDispatchReceiver: IrValueParameterMessageType? = null
        var baseDispatchReceiverOffset: Int = -1
        var baseExtensionReceiver: IrValueParameterMessageType? = null
        var baseExtensionReceiverOffset: Int = -1
        var baseValueParameter: MutableList<IrValueParameterMessageType> = mutableListOf()
        var baseValueParameterOffsetList: MutableList<Int> = arrayListOf()
        var baseBody: Int? = null
        var baseBodyOffset: Int = -1
        var baseReturnType: Int? = null
        var isPrimary: Boolean = false
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> readWithLength {
                                        while (hasData) {
                                            readField { fieldNumber, type ->
                                                when (fieldNumber) {
                                                    1 -> baseBaseSymbol = readWithLength { readIrDataIndex() }
                                                    2 -> baseBaseOrigin = readWithLength { readIrDeclarationOrigin() }
                                                    3 -> readWithLength {
                                                        while (hasData) {
                                                            readField { fieldNumber, type ->
                                                                when (fieldNumber) {
                                                                    1 -> baseBaseCoordinatesStartOffset = readInt32()
                                                                    2 -> baseBaseCoordinatesEndOffset = readInt32()
                                                                    else -> skip(type)
                                                                }
                                                            }
                                                        }
                                                    }
                                                    4 -> baseBaseAnnotations = readWithLength { readAnnotations() }
                                                    else -> skip(type)
                                                }
                                            }
                                        }
                                    }
                                    2 -> baseName = readWithLength { readIrDataIndex() }
                                    3 -> baseVisibility = readWithLength { readVisibility() }
                                    4 -> baseIsInline = readBool()
                                    5 -> baseIsExternal = readBool()
                                    6 -> {
                                        baseTypeParametersOffset = offset
                                        skip(type)
                                    }
                                    7 -> {
                                        baseDispatchReceiverOffset = offset
                                        skip(type)
                                    }
                                    8 -> {
                                        baseExtensionReceiverOffset = offset
                                        skip(type)
                                    }
                                    9 -> {
                                        baseValueParameterOffsetList.add(offset)
                                        skip(type)
                                    }
                                    10 -> {
                                        baseBodyOffset = offset
                                        skip(type)
                                    }
                                    11 -> baseReturnType = readWithLength { readIrDataIndex() }
                                    else -> skip(type)
                                }
                            }
                        }
                    }
                    2 -> isPrimary = readBool()
                    else -> skip(type)
                }
            }
        }
        val p0 = createIrConstructor(baseBaseSymbol!!, baseBaseOrigin!!, baseBaseCoordinatesStartOffset, baseBaseCoordinatesEndOffset, baseBaseAnnotations!!, baseName!!, baseVisibility!!, baseIsInline, baseIsExternal, baseReturnType!!, isPrimary)

        if (baseTypeParametersOffset != -1) {
            baseTypeParameters = delayed(baseTypeParametersOffset) { readWithLength { readIrTypeParameterContainer() } }
        }
        if (baseDispatchReceiverOffset != -1) {
            baseDispatchReceiver = delayed(baseDispatchReceiverOffset) { readWithLength { readIrValueParameter() } }
        }
        if (baseExtensionReceiverOffset != -1) {
            baseExtensionReceiver = delayed(baseExtensionReceiverOffset) { readWithLength { readIrValueParameter() } }
        }
        for (o in baseValueParameterOffsetList) {
            baseValueParameter.add(delayed(o) { readWithLength { readIrValueParameter() } })
        }
        if (baseBodyOffset != -1) {
            baseBody = delayed(baseBodyOffset) { readWithLength { readIrDataIndex() } }
        }
        return createIrConstructor1(p0, baseTypeParameters!!, baseDispatchReceiver, baseExtensionReceiver, baseValueParameter, baseBody)
    }

    open fun readIrField(): IrFieldMessageType {
        var baseSymbol: Int? = null
        var baseOrigin: IrDeclarationOriginMessageType? = null
        var baseCoordinatesStartOffset: Int = 0
        var baseCoordinatesEndOffset: Int = 0
        var baseAnnotations: List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>? = null
        var initializer: Int? = null
        var name: Int? = null
        var visibility: VisibilityMessageType? = null
        var isFinal: Boolean = false
        var isExternal: Boolean = false
        var isStatic: Boolean = false
        var type_: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> baseSymbol = readWithLength { readIrDataIndex() }
                                    2 -> baseOrigin = readWithLength { readIrDeclarationOrigin() }
                                    3 -> readWithLength {
                                        while (hasData) {
                                            readField { fieldNumber, type ->
                                                when (fieldNumber) {
                                                    1 -> baseCoordinatesStartOffset = readInt32()
                                                    2 -> baseCoordinatesEndOffset = readInt32()
                                                    else -> skip(type)
                                                }
                                            }
                                        }
                                    }
                                    4 -> baseAnnotations = readWithLength { readAnnotations() }
                                    else -> skip(type)
                                }
                            }
                        }
                    }
                    2 -> initializer = readWithLength { readIrDataIndex() }
                    3 -> name = readWithLength { readIrDataIndex() }
                    4 -> visibility = readWithLength { readVisibility() }
                    5 -> isFinal = readBool()
                    6 -> isExternal = readBool()
                    7 -> isStatic = readBool()
                    8 -> type_ = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrField(baseSymbol!!, baseOrigin!!, baseCoordinatesStartOffset, baseCoordinatesEndOffset, baseAnnotations!!, initializer, name!!, visibility!!, isFinal, isExternal, isStatic, type_!!)
    }

    open fun readIrLocalDelegatedProperty(): IrLocalDelegatedPropertyMessageType {
        var baseSymbol: Int? = null
        var baseOrigin: IrDeclarationOriginMessageType? = null
        var baseCoordinatesStartOffset: Int = 0
        var baseCoordinatesEndOffset: Int = 0
        var baseAnnotations: List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>? = null
        var name: Int? = null
        var type_: Int? = null
        var isVar: Boolean = false
        var delegate: IrVariableMessageType? = null
        var getter: IrFunctionMessageType? = null
        var setter: IrFunctionMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> baseSymbol = readWithLength { readIrDataIndex() }
                                    2 -> baseOrigin = readWithLength { readIrDeclarationOrigin() }
                                    3 -> readWithLength {
                                        while (hasData) {
                                            readField { fieldNumber, type ->
                                                when (fieldNumber) {
                                                    1 -> baseCoordinatesStartOffset = readInt32()
                                                    2 -> baseCoordinatesEndOffset = readInt32()
                                                    else -> skip(type)
                                                }
                                            }
                                        }
                                    }
                                    4 -> baseAnnotations = readWithLength { readAnnotations() }
                                    else -> skip(type)
                                }
                            }
                        }
                    }
                    2 -> name = readWithLength { readIrDataIndex() }
                    3 -> type_ = readWithLength { readIrDataIndex() }
                    4 -> isVar = readBool()
                    5 -> delegate = readWithLength { readIrVariable() }
                    6 -> getter = readWithLength { readIrFunction() }
                    7 -> setter = readWithLength { readIrFunction() }
                    else -> skip(type)
                }
            }
        }
        return createIrLocalDelegatedProperty(baseSymbol!!, baseOrigin!!, baseCoordinatesStartOffset, baseCoordinatesEndOffset, baseAnnotations!!, name!!, type_!!, isVar, delegate!!, getter, setter)
    }

    open fun readIrProperty(): IrPropertyMessageType {
        var baseSymbol: Int? = null
        var baseOrigin: IrDeclarationOriginMessageType? = null
        var baseCoordinatesStartOffset: Int = 0
        var baseCoordinatesEndOffset: Int = 0
        var baseAnnotations: List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>? = null
        var name: Int? = null
        var visibility: VisibilityMessageType? = null
        var modality: ModalityKindMessageType? = null
        var isVar: Boolean = false
        var isConst: Boolean = false
        var isLateinit: Boolean = false
        var isDelegated: Boolean = false
        var isExternal: Boolean = false
        var backingField: IrFieldMessageType? = null
        var getter: IrFunctionMessageType? = null
        var setter: IrFunctionMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> baseSymbol = readWithLength { readIrDataIndex() }
                                    2 -> baseOrigin = readWithLength { readIrDeclarationOrigin() }
                                    3 -> readWithLength {
                                        while (hasData) {
                                            readField { fieldNumber, type ->
                                                when (fieldNumber) {
                                                    1 -> baseCoordinatesStartOffset = readInt32()
                                                    2 -> baseCoordinatesEndOffset = readInt32()
                                                    else -> skip(type)
                                                }
                                            }
                                        }
                                    }
                                    4 -> baseAnnotations = readWithLength { readAnnotations() }
                                    else -> skip(type)
                                }
                            }
                        }
                    }
                    2 -> name = readWithLength { readIrDataIndex() }
                    3 -> visibility = readWithLength { readVisibility() }
                    4 -> modality = createModalityKind(readInt32())
                    5 -> isVar = readBool()
                    6 -> isConst = readBool()
                    7 -> isLateinit = readBool()
                    8 -> isDelegated = readBool()
                    9 -> isExternal = readBool()
                    10 -> backingField = readWithLength { readIrField() }
                    11 -> getter = readWithLength { readIrFunction() }
                    12 -> setter = readWithLength { readIrFunction() }
                    else -> skip(type)
                }
            }
        }
        return createIrProperty(baseSymbol!!, baseOrigin!!, baseCoordinatesStartOffset, baseCoordinatesEndOffset, baseAnnotations!!, name!!, visibility!!, modality!!, isVar, isConst, isLateinit, isDelegated, isExternal, backingField, getter, setter)
    }

    open fun readIrVariable(): IrVariableMessageType {
        var baseSymbol: Int? = null
        var baseOrigin: IrDeclarationOriginMessageType? = null
        var baseCoordinatesStartOffset: Int = 0
        var baseCoordinatesEndOffset: Int = 0
        var baseAnnotations: List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>? = null
        var name: Int? = null
        var type_: Int? = null
        var isVar: Boolean = false
        var isConst: Boolean = false
        var isLateinit: Boolean = false
        var initializer: IrExpressionMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> baseSymbol = readWithLength { readIrDataIndex() }
                                    2 -> baseOrigin = readWithLength { readIrDeclarationOrigin() }
                                    3 -> readWithLength {
                                        while (hasData) {
                                            readField { fieldNumber, type ->
                                                when (fieldNumber) {
                                                    1 -> baseCoordinatesStartOffset = readInt32()
                                                    2 -> baseCoordinatesEndOffset = readInt32()
                                                    else -> skip(type)
                                                }
                                            }
                                        }
                                    }
                                    4 -> baseAnnotations = readWithLength { readAnnotations() }
                                    else -> skip(type)
                                }
                            }
                        }
                    }
                    2 -> name = readWithLength { readIrDataIndex() }
                    3 -> type_ = readWithLength { readIrDataIndex() }
                    4 -> isVar = readBool()
                    5 -> isConst = readBool()
                    6 -> isLateinit = readBool()
                    7 -> initializer = readWithLength { readIrExpression() }
                    else -> skip(type)
                }
            }
        }
        return createIrVariable(baseSymbol!!, baseOrigin!!, baseCoordinatesStartOffset, baseCoordinatesEndOffset, baseAnnotations!!, name!!, type_!!, isVar, isConst, isLateinit, initializer)
    }

    open fun readIrValueParameter(): IrValueParameterMessageType {
        var baseSymbol: Int? = null
        var baseOrigin: IrDeclarationOriginMessageType? = null
        var baseCoordinatesStartOffset: Int = 0
        var baseCoordinatesEndOffset: Int = 0
        var baseAnnotations: List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>? = null
        var name: Int? = null
        var index: Int = 0
        var type_: Int? = null
        var varargElementType: Int? = null
        var isCrossinline: Boolean = false
        var isNoinline: Boolean = false
        var defaultValue: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> baseSymbol = readWithLength { readIrDataIndex() }
                                    2 -> baseOrigin = readWithLength { readIrDeclarationOrigin() }
                                    3 -> readWithLength {
                                        while (hasData) {
                                            readField { fieldNumber, type ->
                                                when (fieldNumber) {
                                                    1 -> baseCoordinatesStartOffset = readInt32()
                                                    2 -> baseCoordinatesEndOffset = readInt32()
                                                    else -> skip(type)
                                                }
                                            }
                                        }
                                    }
                                    4 -> baseAnnotations = readWithLength { readAnnotations() }
                                    else -> skip(type)
                                }
                            }
                        }
                    }
                    2 -> name = readWithLength { readIrDataIndex() }
                    3 -> index = readInt32()
                    4 -> type_ = readWithLength { readIrDataIndex() }
                    5 -> varargElementType = readWithLength { readIrDataIndex() }
                    6 -> isCrossinline = readBool()
                    7 -> isNoinline = readBool()
                    8 -> defaultValue = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrValueParameter(baseSymbol!!, baseOrigin!!, baseCoordinatesStartOffset, baseCoordinatesEndOffset, baseAnnotations!!, name!!, index, type_!!, varargElementType, isCrossinline, isNoinline, defaultValue)
    }

    open fun readIrTypeParameter(): IrTypeParameterMessageType {
        var baseSymbol: Int? = null
        var baseOrigin: IrDeclarationOriginMessageType? = null
        var baseCoordinatesStartOffset: Int = 0
        var baseCoordinatesEndOffset: Int = 0
        var baseAnnotations: List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>? = null
        var name: Int? = null
        var index: Int = 0
        var variance: IrTypeVarianceMessageType? = null
        var superType: MutableList<Int> = mutableListOf()
        var isReified: Boolean = false
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> baseSymbol = readWithLength { readIrDataIndex() }
                                    2 -> baseOrigin = readWithLength { readIrDeclarationOrigin() }
                                    3 -> readWithLength {
                                        while (hasData) {
                                            readField { fieldNumber, type ->
                                                when (fieldNumber) {
                                                    1 -> baseCoordinatesStartOffset = readInt32()
                                                    2 -> baseCoordinatesEndOffset = readInt32()
                                                    else -> skip(type)
                                                }
                                            }
                                        }
                                    }
                                    4 -> baseAnnotations = readWithLength { readAnnotations() }
                                    else -> skip(type)
                                }
                            }
                        }
                    }
                    2 -> name = readWithLength { readIrDataIndex() }
                    3 -> index = readInt32()
                    4 -> variance = createIrTypeVariance(readInt32())
                    5 -> superType.add(readWithLength { readIrDataIndex() })
                    6 -> isReified = readBool()
                    else -> skip(type)
                }
            }
        }
        return createIrTypeParameter(baseSymbol!!, baseOrigin!!, baseCoordinatesStartOffset, baseCoordinatesEndOffset, baseAnnotations!!, name!!, index, variance!!, superType, isReified)
    }

    open fun readIrTypeParameterContainer(): List<org.jetbrains.kotlin.ir.declarations.IrTypeParameter> {
        var typeParameter: MutableList<IrTypeParameterMessageType> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> typeParameter.add(readWithLength { readIrTypeParameter() })
                    else -> skip(type)
                }
            }
        }
        return createIrTypeParameterContainer(typeParameter)
    }

    open fun readIrClass(): IrClassMessageType {
        var baseSymbol: Int? = null
        var baseOrigin: IrDeclarationOriginMessageType? = null
        var baseCoordinatesStartOffset: Int = 0
        var baseCoordinatesEndOffset: Int = 0
        var baseAnnotations: List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>? = null
        var name: Int? = null
        var kind: ClassKindMessageType? = null
        var visibility: VisibilityMessageType? = null
        var modality: ModalityKindMessageType? = null
        var isCompanion: Boolean = false
        var isInner: Boolean = false
        var isData: Boolean = false
        var isExternal: Boolean = false
        var isInline: Boolean = false
        var thisReceiver: IrValueParameterMessageType? = null
        var thisReceiverOffset: Int = -1
        var typeParameters: List<org.jetbrains.kotlin.ir.declarations.IrTypeParameter>? = null
        var typeParametersOffset: Int = -1
        var declarationContainer: List<org.jetbrains.kotlin.ir.declarations.IrDeclaration>? = null
        var declarationContainerOffset: Int = -1
        var superType: MutableList<Int> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> baseSymbol = readWithLength { readIrDataIndex() }
                                    2 -> baseOrigin = readWithLength { readIrDeclarationOrigin() }
                                    3 -> readWithLength {
                                        while (hasData) {
                                            readField { fieldNumber, type ->
                                                when (fieldNumber) {
                                                    1 -> baseCoordinatesStartOffset = readInt32()
                                                    2 -> baseCoordinatesEndOffset = readInt32()
                                                    else -> skip(type)
                                                }
                                            }
                                        }
                                    }
                                    4 -> baseAnnotations = readWithLength { readAnnotations() }
                                    else -> skip(type)
                                }
                            }
                        }
                    }
                    2 -> name = readWithLength { readIrDataIndex() }
                    3 -> kind = createClassKind(readInt32())
                    4 -> visibility = readWithLength { readVisibility() }
                    5 -> modality = createModalityKind(readInt32())
                    6 -> isCompanion = readBool()
                    7 -> isInner = readBool()
                    8 -> isData = readBool()
                    9 -> isExternal = readBool()
                    10 -> isInline = readBool()
                    11 -> {
                        thisReceiverOffset = offset
                        skip(type)
                    }
                    12 -> {
                        typeParametersOffset = offset
                        skip(type)
                    }
                    13 -> {
                        declarationContainerOffset = offset
                        skip(type)
                    }
                    14 -> superType.add(readWithLength { readIrDataIndex() })
                    else -> skip(type)
                }
            }
        }
        val p0 = createIrClass(baseSymbol!!, baseOrigin!!, baseCoordinatesStartOffset, baseCoordinatesEndOffset, baseAnnotations!!, name!!, kind!!, visibility!!, modality!!, isCompanion, isInner, isData, isExternal, isInline, superType)

        if (thisReceiverOffset != -1) {
            thisReceiver = delayed(thisReceiverOffset) { readWithLength { readIrValueParameter() } }
        }
        if (typeParametersOffset != -1) {
            typeParameters = delayed(typeParametersOffset) { readWithLength { readIrTypeParameterContainer() } }
        }
        val p1 = createIrClass1(p0, thisReceiver, typeParameters!!)

        if (declarationContainerOffset != -1) {
            declarationContainer = delayed(declarationContainerOffset) { readWithLength { readIrDeclarationContainer() } }
        }
        return createIrClass2(p1, declarationContainer!!)
    }

    open fun readIrTypeAlias(): IrTypeAliasMessageType {
        var baseSymbol: Int? = null
        var baseOrigin: IrDeclarationOriginMessageType? = null
        var baseCoordinatesStartOffset: Int = 0
        var baseCoordinatesEndOffset: Int = 0
        var baseAnnotations: List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>? = null
        var name: Int? = null
        var visibility: VisibilityMessageType? = null
        var typeParameters: List<org.jetbrains.kotlin.ir.declarations.IrTypeParameter>? = null
        var expandedType: Int? = null
        var isActual: Boolean = false
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> baseSymbol = readWithLength { readIrDataIndex() }
                                    2 -> baseOrigin = readWithLength { readIrDeclarationOrigin() }
                                    3 -> readWithLength {
                                        while (hasData) {
                                            readField { fieldNumber, type ->
                                                when (fieldNumber) {
                                                    1 -> baseCoordinatesStartOffset = readInt32()
                                                    2 -> baseCoordinatesEndOffset = readInt32()
                                                    else -> skip(type)
                                                }
                                            }
                                        }
                                    }
                                    4 -> baseAnnotations = readWithLength { readAnnotations() }
                                    else -> skip(type)
                                }
                            }
                        }
                    }
                    2 -> name = readWithLength { readIrDataIndex() }
                    3 -> visibility = readWithLength { readVisibility() }
                    4 -> typeParameters = readWithLength { readIrTypeParameterContainer() }
                    5 -> expandedType = readWithLength { readIrDataIndex() }
                    6 -> isActual = readBool()
                    else -> skip(type)
                }
            }
        }
        return createIrTypeAlias(baseSymbol!!, baseOrigin!!, baseCoordinatesStartOffset, baseCoordinatesEndOffset, baseAnnotations!!, name!!, visibility!!, typeParameters!!, expandedType!!, isActual)
    }

    open fun readIrEnumEntry(): IrEnumEntryMessageType {
        var baseSymbol: Int? = null
        var baseOrigin: IrDeclarationOriginMessageType? = null
        var baseCoordinatesStartOffset: Int = 0
        var baseCoordinatesEndOffset: Int = 0
        var baseAnnotations: List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>? = null
        var initializer: Int? = null
        var correspondingClass: IrClassMessageType? = null
        var name: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> baseSymbol = readWithLength { readIrDataIndex() }
                                    2 -> baseOrigin = readWithLength { readIrDeclarationOrigin() }
                                    3 -> readWithLength {
                                        while (hasData) {
                                            readField { fieldNumber, type ->
                                                when (fieldNumber) {
                                                    1 -> baseCoordinatesStartOffset = readInt32()
                                                    2 -> baseCoordinatesEndOffset = readInt32()
                                                    else -> skip(type)
                                                }
                                            }
                                        }
                                    }
                                    4 -> baseAnnotations = readWithLength { readAnnotations() }
                                    else -> skip(type)
                                }
                            }
                        }
                    }
                    2 -> initializer = readWithLength { readIrDataIndex() }
                    3 -> correspondingClass = readWithLength { readIrClass() }
                    4 -> name = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrEnumEntry(baseSymbol!!, baseOrigin!!, baseCoordinatesStartOffset, baseCoordinatesEndOffset, baseAnnotations!!, initializer, correspondingClass, name!!)
    }

    open fun readIrAnonymousInit(): IrAnonymousInitMessageType {
        var baseSymbol: Int? = null
        var baseOrigin: IrDeclarationOriginMessageType? = null
        var baseCoordinatesStartOffset: Int = 0
        var baseCoordinatesEndOffset: Int = 0
        var baseAnnotations: List<org.jetbrains.kotlin.ir.expressions.IrConstructorCall>? = null
        var body: Int? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> baseSymbol = readWithLength { readIrDataIndex() }
                                    2 -> baseOrigin = readWithLength { readIrDeclarationOrigin() }
                                    3 -> readWithLength {
                                        while (hasData) {
                                            readField { fieldNumber, type ->
                                                when (fieldNumber) {
                                                    1 -> baseCoordinatesStartOffset = readInt32()
                                                    2 -> baseCoordinatesEndOffset = readInt32()
                                                    else -> skip(type)
                                                }
                                            }
                                        }
                                    }
                                    4 -> baseAnnotations = readWithLength { readAnnotations() }
                                    else -> skip(type)
                                }
                            }
                        }
                    }
                    2 -> body = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrAnonymousInit(baseSymbol!!, baseOrigin!!, baseCoordinatesStartOffset, baseCoordinatesEndOffset, baseAnnotations!!, body!!)
    }

    open fun readIrDeclaration(): IrDeclarationMessageType {
        var oneOfIrAnonymousInit: IrAnonymousInitMessageType? = null
        var oneOfIrClass: IrClassMessageType? = null
        var oneOfIrConstructor: IrConstructorMessageType? = null
        var oneOfIrEnumEntry: IrEnumEntryMessageType? = null
        var oneOfIrField: IrFieldMessageType? = null
        var oneOfIrFunction: IrFunctionMessageType? = null
        var oneOfIrProperty: IrPropertyMessageType? = null
        var oneOfIrTypeParameter: IrTypeParameterMessageType? = null
        var oneOfIrVariable: IrVariableMessageType? = null
        var oneOfIrValueParameter: IrValueParameterMessageType? = null
        var oneOfIrLocalDelegatedProperty: IrLocalDelegatedPropertyMessageType? = null
        var oneOfIrTypeAlias: IrTypeAliasMessageType? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> {
                        oneOfIrAnonymousInit = readWithLength { readIrAnonymousInit() }
                        oneOfIndex = 1
                    }
                    2 -> {
                        oneOfIrClass = readWithLength { readIrClass() }
                        oneOfIndex = 2
                    }
                    3 -> {
                        oneOfIrConstructor = readWithLength { readIrConstructor() }
                        oneOfIndex = 3
                    }
                    4 -> {
                        oneOfIrEnumEntry = readWithLength { readIrEnumEntry() }
                        oneOfIndex = 4
                    }
                    5 -> {
                        oneOfIrField = readWithLength { readIrField() }
                        oneOfIndex = 5
                    }
                    6 -> {
                        oneOfIrFunction = readWithLength { readIrFunction() }
                        oneOfIndex = 6
                    }
                    7 -> {
                        oneOfIrProperty = readWithLength { readIrProperty() }
                        oneOfIndex = 7
                    }
                    8 -> {
                        oneOfIrTypeParameter = readWithLength { readIrTypeParameter() }
                        oneOfIndex = 8
                    }
                    9 -> {
                        oneOfIrVariable = readWithLength { readIrVariable() }
                        oneOfIndex = 9
                    }
                    10 -> {
                        oneOfIrValueParameter = readWithLength { readIrValueParameter() }
                        oneOfIndex = 10
                    }
                    11 -> {
                        oneOfIrLocalDelegatedProperty = readWithLength { readIrLocalDelegatedProperty() }
                        oneOfIndex = 11
                    }
                    12 -> {
                        oneOfIrTypeAlias = readWithLength { readIrTypeAlias() }
                        oneOfIndex = 12
                    }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            1 -> return createIrDeclaration_irAnonymousInit(oneOfIrAnonymousInit!!)
            2 -> return createIrDeclaration_irClass(oneOfIrClass!!)
            3 -> return createIrDeclaration_irConstructor(oneOfIrConstructor!!)
            4 -> return createIrDeclaration_irEnumEntry(oneOfIrEnumEntry!!)
            5 -> return createIrDeclaration_irField(oneOfIrField!!)
            6 -> return createIrDeclaration_irFunction(oneOfIrFunction!!)
            7 -> return createIrDeclaration_irProperty(oneOfIrProperty!!)
            8 -> return createIrDeclaration_irTypeParameter(oneOfIrTypeParameter!!)
            9 -> return createIrDeclaration_irVariable(oneOfIrVariable!!)
            10 -> return createIrDeclaration_irValueParameter(oneOfIrValueParameter!!)
            11 -> return createIrDeclaration_irLocalDelegatedProperty(oneOfIrLocalDelegatedProperty!!)
            12 -> return createIrDeclaration_irTypeAlias(oneOfIrTypeAlias!!)
            else -> error("Incorrect oneOf index: " + oneOfIndex)
        }
    }

    open fun readIrBranch(): IrBranchMessageType {
        var condition: IrExpressionMessageType? = null
        var result: IrExpressionMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> condition = readWithLength { readIrExpression() }
                    2 -> result = readWithLength { readIrExpression() }
                    else -> skip(type)
                }
            }
        }
        return createIrBranch(condition!!, result!!)
    }

    open fun readIrBlockBody(): IrBlockBodyMessageType {
        var statement: MutableList<IrStatementMessageType> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> statement.add(readWithLength { readIrStatement() })
                    else -> skip(type)
                }
            }
        }
        return createIrBlockBody(statement)
    }

    open fun readIrCatch(): IrCatchMessageType {
        var catchParameter: IrVariableMessageType? = null
        var result: IrExpressionMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> catchParameter = readWithLength { readIrVariable() }
                    2 -> result = readWithLength { readIrExpression() }
                    else -> skip(type)
                }
            }
        }
        return createIrCatch(catchParameter!!, result!!)
    }

    open fun readIrSyntheticBody(): IrSyntheticBodyMessageType {
        var kind: IrSyntheticBodyKindMessageType? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> kind = createIrSyntheticBodyKind(readInt32())
                    else -> skip(type)
                }
            }
        }
        return createIrSyntheticBody(kind!!)
    }

    protected var fieldIrStatementCoordinatesStartOffset: Int = 0
    protected var fieldIrStatementCoordinatesEndOffset: Int = 0

    open fun readIrStatement(): IrStatementMessageType {
        var coordinatesStartOffset: Int = 0
        var coordinatesEndOffset: Int = 0
        var oneOfDeclaration: IrDeclarationMessageType? = null
        var oneOfDeclarationOffset: Int = -1
        var oneOfExpression: IrExpressionMessageType? = null
        var oneOfExpressionOffset: Int = -1
        var oneOfBlockBody: IrBlockBodyMessageType? = null
        var oneOfBlockBodyOffset: Int = -1
        var oneOfBranch: IrBranchMessageType? = null
        var oneOfBranchOffset: Int = -1
        var oneOfCatch: IrCatchMessageType? = null
        var oneOfCatchOffset: Int = -1
        var oneOfSyntheticBody: IrSyntheticBodyMessageType? = null
        var oneOfSyntheticBodyOffset: Int = -1
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> coordinatesStartOffset = readInt32()
                                    2 -> coordinatesEndOffset = readInt32()
                                    else -> skip(type)
                                }
                            }
                        }
                    }
                    2 -> {
                        oneOfDeclarationOffset = offset
                        skip(type)
                        oneOfIndex = 2
                    }
                    3 -> {
                        oneOfExpressionOffset = offset
                        skip(type)
                        oneOfIndex = 3
                    }
                    4 -> {
                        oneOfBlockBodyOffset = offset
                        skip(type)
                        oneOfIndex = 4
                    }
                    5 -> {
                        oneOfBranchOffset = offset
                        skip(type)
                        oneOfIndex = 5
                    }
                    6 -> {
                        oneOfCatchOffset = offset
                        skip(type)
                        oneOfIndex = 6
                    }
                    7 -> {
                        oneOfSyntheticBodyOffset = offset
                        skip(type)
                        oneOfIndex = 7
                    }
                    else -> skip(type)
                }
            }
        }
        val oldfieldIrStatementCoordinatesStartOffset = fieldIrStatementCoordinatesStartOffset
        fieldIrStatementCoordinatesStartOffset = coordinatesStartOffset
        val oldfieldIrStatementCoordinatesEndOffset = fieldIrStatementCoordinatesEndOffset
        fieldIrStatementCoordinatesEndOffset = coordinatesEndOffset
        if (oneOfDeclarationOffset != -1) {
            oneOfDeclaration = delayed(oneOfDeclarationOffset) { readWithLength { readIrDeclaration() } }
        }
        if (oneOfExpressionOffset != -1) {
            oneOfExpression = delayed(oneOfExpressionOffset) { readWithLength { readIrExpression() } }
        }
        if (oneOfBlockBodyOffset != -1) {
            oneOfBlockBody = delayed(oneOfBlockBodyOffset) { readWithLength { readIrBlockBody() } }
        }
        if (oneOfBranchOffset != -1) {
            oneOfBranch = delayed(oneOfBranchOffset) { readWithLength { readIrBranch() } }
        }
        if (oneOfCatchOffset != -1) {
            oneOfCatch = delayed(oneOfCatchOffset) { readWithLength { readIrCatch() } }
        }
        if (oneOfSyntheticBodyOffset != -1) {
            oneOfSyntheticBody = delayed(oneOfSyntheticBodyOffset) { readWithLength { readIrSyntheticBody() } }
        }
        val p0 = when (oneOfIndex) {
            2 -> createIrStatement_declaration(coordinatesStartOffset, coordinatesEndOffset, oneOfDeclaration!!)
            3 -> createIrStatement_expression(coordinatesStartOffset, coordinatesEndOffset, oneOfExpression!!)
            4 -> createIrStatement_blockBody(coordinatesStartOffset, coordinatesEndOffset, oneOfBlockBody!!)
            5 -> createIrStatement_branch(coordinatesStartOffset, coordinatesEndOffset, oneOfBranch!!)
            6 -> createIrStatement_catch(coordinatesStartOffset, coordinatesEndOffset, oneOfCatch!!)
            7 -> createIrStatement_syntheticBody(coordinatesStartOffset, coordinatesEndOffset, oneOfSyntheticBody!!)
            else -> error("Incorrect oneOf index: " + oneOfIndex)
        }
        fieldIrStatementCoordinatesStartOffset = oldfieldIrStatementCoordinatesStartOffset
        fieldIrStatementCoordinatesEndOffset = oldfieldIrStatementCoordinatesEndOffset
        return p0
    }

}
