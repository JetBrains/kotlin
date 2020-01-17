/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.ir.DeclarationFactory
import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.common.serialization.proto.IrConst.ValueCase.*
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration.DeclaratorCase.*
import org.jetbrains.kotlin.backend.common.serialization.proto.IrOperation.OperationCase.*
import org.jetbrains.kotlin.backend.common.serialization.proto.IrStatement.StatementCase
import org.jetbrains.kotlin.backend.common.serialization.proto.IrType.KindCase.*
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeArgument.KindCase.STAR
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeArgument.KindCase.TYPE
import org.jetbrains.kotlin.backend.common.serialization.proto.IrVarargElement.VarargElementCase
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedTypeParameterDescriptor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.backend.common.serialization.proto.ClassKind as ProtoClassKind
import org.jetbrains.kotlin.backend.common.serialization.proto.DescriptorReference as ProtoDescriptorReference
import org.jetbrains.kotlin.backend.common.serialization.proto.IrAnonymousInit as ProtoAnonymousInit
import org.jetbrains.kotlin.backend.common.serialization.proto.IrBlock as ProtoBlock
//import org.jetbrains.kotlin.backend.common.serialization.proto.IrDataIndex as ProtoBodyIndex
import org.jetbrains.kotlin.backend.common.serialization.proto.IrBlockBody as ProtoBlockBody
import org.jetbrains.kotlin.backend.common.serialization.proto.IrBranch as ProtoBranch
import org.jetbrains.kotlin.backend.common.serialization.proto.IrBreak as ProtoBreak
import org.jetbrains.kotlin.backend.common.serialization.proto.IrCall as ProtoCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrCatch as ProtoCatch
import org.jetbrains.kotlin.backend.common.serialization.proto.IrClass as ProtoClass
import org.jetbrains.kotlin.backend.common.serialization.proto.IrClassReference as ProtoClassReference
import org.jetbrains.kotlin.backend.common.serialization.proto.IrComposite as ProtoComposite
import org.jetbrains.kotlin.backend.common.serialization.proto.IrConst as ProtoConst
import org.jetbrains.kotlin.backend.common.serialization.proto.IrConstructor as ProtoConstructor
import org.jetbrains.kotlin.backend.common.serialization.proto.IrConstructorCall as ProtoConstructorCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrContinue as ProtoContinue
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoDeclaration
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase as ProtoDeclarationBase
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationOrigin as ProtoDeclarationOrigin
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDelegatingConstructorCall as ProtoDelegatingConstructorCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDoWhile as ProtoDoWhile
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDynamicMemberExpression as ProtoDynamicMemberExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDynamicOperatorExpression as ProtoDynamicOperatorExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDynamicType as ProtoDynamicType
import org.jetbrains.kotlin.backend.common.serialization.proto.IrEnumConstructorCall as ProtoEnumConstructorCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrEnumEntry as ProtoEnumEntry
import org.jetbrains.kotlin.backend.common.serialization.proto.IrErrorType as ProtoErrorType
import org.jetbrains.kotlin.backend.common.serialization.proto.IrExpression as ProtoExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrField as ProtoField
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFunction as ProtoFunction
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFunctionBase as ProtoFunctionBase
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFunctionExpression as ProtoFunctionExpression
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFunctionReference as ProtoFunctionReference
import org.jetbrains.kotlin.backend.common.serialization.proto.IrGetClass as ProtoGetClass
import org.jetbrains.kotlin.backend.common.serialization.proto.IrGetEnumValue as ProtoGetEnumValue
import org.jetbrains.kotlin.backend.common.serialization.proto.IrGetField as ProtoGetField
import org.jetbrains.kotlin.backend.common.serialization.proto.IrGetObject as ProtoGetObject
import org.jetbrains.kotlin.backend.common.serialization.proto.IrGetValue as ProtoGetValue
import org.jetbrains.kotlin.backend.common.serialization.proto.IrInstanceInitializerCall as ProtoInstanceInitializerCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrLocalDelegatedProperty as ProtoLocalDelegatedProperty
import org.jetbrains.kotlin.backend.common.serialization.proto.IrLocalDelegatedPropertyReference as ProtoLocalDelegatedPropertyReference
import org.jetbrains.kotlin.backend.common.serialization.proto.IrOperation as ProtoOperation
import org.jetbrains.kotlin.backend.common.serialization.proto.IrProperty as ProtoProperty
import org.jetbrains.kotlin.backend.common.serialization.proto.IrPropertyReference as ProtoPropertyReference
import org.jetbrains.kotlin.backend.common.serialization.proto.IrReturn as ProtoReturn
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSetField as ProtoSetField
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSetVariable as ProtoSetVariable
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSimpleType as ProtoSimpleType
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSpreadElement as ProtoSpreadElement
import org.jetbrains.kotlin.backend.common.serialization.proto.IrStatement as ProtoStatement
import org.jetbrains.kotlin.backend.common.serialization.proto.IrStatementOrigin as ProtoStatementOrigin
import org.jetbrains.kotlin.backend.common.serialization.proto.IrStringConcat as ProtoStringConcat
//import org.jetbrains.kotlin.backend.common.serialization.proto.IrDataIndex as ProtoSymbolIndex
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSyntheticBody as ProtoSyntheticBody
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSyntheticBodyKind as ProtoSyntheticBodyKind
import org.jetbrains.kotlin.backend.common.serialization.proto.IrThrow as ProtoThrow
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTry as ProtoTry
import org.jetbrains.kotlin.backend.common.serialization.proto.IrType as ProtoType
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeAbbreviation as ProtoTypeAbbreviation
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeAlias as ProtoTypeAlias
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeArgument as ProtoTypeArgument
//import org.jetbrains.kotlin.backend.common.serialization.proto.IrDataIndex as ProtoTypeIndex
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeOp as ProtoTypeOp
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeOperator as ProtoTypeOperator
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeParameter as ProtoTypeParameter
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeVariance as ProtoTypeVariance
import org.jetbrains.kotlin.backend.common.serialization.proto.IrValueParameter as ProtoValueParameter
import org.jetbrains.kotlin.backend.common.serialization.proto.IrVararg as ProtoVararg
import org.jetbrains.kotlin.backend.common.serialization.proto.IrVarargElement as ProtoVarargElement
import org.jetbrains.kotlin.backend.common.serialization.proto.IrVariable as ProtoVariable
import org.jetbrains.kotlin.backend.common.serialization.proto.IrWhen as ProtoWhen
import org.jetbrains.kotlin.backend.common.serialization.proto.IrWhile as ProtoWhile
import org.jetbrains.kotlin.backend.common.serialization.proto.Loop as ProtoLoop
import org.jetbrains.kotlin.backend.common.serialization.proto.MemberAccessCommon as ProtoMemberAccessCommon
import org.jetbrains.kotlin.backend.common.serialization.proto.ModalityKind as ProtoModalityKind
//import org.jetbrains.kotlin.backend.common.serialization.proto.IrDataIndex as ProtoStringIndex
import org.jetbrains.kotlin.backend.common.serialization.proto.TypeArguments as ProtoTypeArguments
import org.jetbrains.kotlin.backend.common.serialization.proto.Visibility as ProtoVisibility
//import org.jetbrains.kotlin.backend.common.serialization.proto.FqName as ProtoFqName

// TODO: This code still has some uses of descriptors:
// 1. We use descriptors as keys for symbolTable -- probably symbol table related code should be refactored out from
// the deserializer.
// 2. Properties use descriptors but not symbols -- that causes lots of assymmetry all around.
// 3. Declarations are provided with wrapped descriptors. That is probably a legitimate descriptor use.

abstract class IrFileDeserializer(
    val logger: LoggingContext,
    val builtIns: IrBuiltIns,
    val symbolTable: SymbolTable
) {

    abstract fun deserializeIrSymbol(index: Int): IrSymbol
    abstract fun deserializeIrType(index: Int): IrType
    abstract fun deserializeDescriptorReference(proto: ProtoDescriptorReference): DeclarationDescriptor
    abstract fun deserializeString(index: Int): String
    abstract fun deserializeExpressionBody(index: Int): IrExpression
    abstract fun deserializeStatementBody(index: Int): IrElement
    abstract fun deserializeLoopHeader(loopIndex: Int, loopBuilder: () -> IrLoopBase): IrLoopBase

    private val parentsStack = mutableListOf<IrDeclarationParent>()

    fun deserializeFqName(fqn: List<Int>): FqName {
        return fqn.run {
            if (isEmpty()) FqName.ROOT else FqName.fromSegments(map { deserializeString(it) })
        }
    }

    private fun deserializeName(index: Int): Name {
        val name = deserializeString(index)
        return Name.guessByFirstCharacter(name)
    }

    private fun deserializeTypeArguments(proto: ProtoTypeArguments): List<IrType> {
        logger.log { "### deserializeTypeArguments" }
        val result = mutableListOf<IrType>()
        proto.typeArgumentList.forEach { typeProto ->
            val type = deserializeIrType(typeProto)
            result.add(type)
            logger.log { "$type" }
        }
        return result
    }

    private fun deserializeIrTypeVariance(variance: ProtoTypeVariance) = when (variance) {
        ProtoTypeVariance.IN -> Variance.IN_VARIANCE
        ProtoTypeVariance.OUT -> Variance.OUT_VARIANCE
        ProtoTypeVariance.INV -> Variance.INVARIANT
    }

    private fun deserializeIrTypeArgument(proto: ProtoTypeArgument) = when (proto.kindCase) {
        STAR -> IrStarProjectionImpl
        TYPE -> makeTypeProjection(
            deserializeIrType(proto.type.type), deserializeIrTypeVariance(proto.type.variance)
        )
        else -> TODO("Unexpected projection kind")

    }

    fun deserializeAnnotations(annotations: List<ProtoConstructorCall>): List<IrConstructorCall> {
        return annotations.map {
            deserializeConstructorCall(it, 0, 0, builtIns.unitType) // TODO: need a proper deserialization here
        }
    }

    private fun deserializeSimpleType(proto: ProtoSimpleType): IrSimpleType {
        val symbol = deserializeIrSymbol(proto.classifier) as? IrClassifierSymbol
            ?: error("could not convert sym to ClassifierSymbol")
        logger.log { "deserializeSimpleType: symbol=$symbol" }

        val arguments = proto.argumentList.map { deserializeIrTypeArgument(it) }
        val annotations = deserializeAnnotations(proto.annotationList)

        val result: IrSimpleType = IrSimpleTypeImpl(
            null,
            symbol,
            proto.hasQuestionMark,
            arguments,
            annotations,
            if (proto.hasAbbreviation()) deserializeTypeAbbreviation(proto.abbreviation) else null
        )
        logger.log { "ir_type = $result; render = ${result.render()}" }
        return result

    }

    private fun deserializeTypeAbbreviation(proto: ProtoTypeAbbreviation): IrTypeAbbreviation =
        IrTypeAbbreviationImpl(
            deserializeIrSymbol(proto.typeAlias).let {
                it as? IrTypeAliasSymbol
                    ?: error("IrTypeAliasSymbol expected: $it")
            },
            proto.hasQuestionMark,
            proto.argumentList.map { deserializeIrTypeArgument(it) },
            deserializeAnnotations(proto.annotationList)
        )

    private fun deserializeDynamicType(proto: ProtoDynamicType): IrDynamicType {
        val annotations = deserializeAnnotations(proto.annotationList)
        return IrDynamicTypeImpl(null, annotations, Variance.INVARIANT)
    }

    private fun deserializeErrorType(proto: ProtoErrorType): IrErrorType {
        val annotations = deserializeAnnotations(proto.annotationList)
        return IrErrorTypeImpl(null, annotations, Variance.INVARIANT)
    }

    fun deserializeIrTypeData(proto: ProtoType): IrType {
        return when (proto.kindCase) {
            SIMPLE -> deserializeSimpleType(proto.simple)
            DYNAMIC -> deserializeDynamicType(proto.dynamic)
            ERROR -> deserializeErrorType(proto.error)
            else -> TODO("Unexpected IrType kind: ${proto.kindCase}")
        }
    }

    /* -------------------------------------------------------------- */

    private fun deserializeBlockBody(
        proto: ProtoBlockBody,
        start: Int, end: Int
    ): IrBlockBody {

        val statements = mutableListOf<IrStatement>()

        val statementProtos = proto.statementList
        statementProtos.forEach {
            statements.add(deserializeStatement(it) as IrStatement)
        }

        return IrBlockBodyImpl(start, end, statements)
    }

    private fun deserializeBranch(proto: ProtoBranch, start: Int, end: Int): IrBranch {

        val condition = deserializeExpression(proto.condition)
        val result = deserializeExpression(proto.result)

        return IrBranchImpl(start, end, condition, result)
    }

    private fun deserializeCatch(proto: ProtoCatch, start: Int, end: Int): IrCatch {
        val catchParameter = deserializeIrVariable(proto.catchParameter)
        val result = deserializeExpression(proto.result)

        return IrCatchImpl(start, end, catchParameter, result)
    }

    private fun deserializeSyntheticBody(proto: ProtoSyntheticBody, start: Int, end: Int): IrSyntheticBody {
        val kind = when (proto.kind!!) {
            ProtoSyntheticBodyKind.ENUM_VALUES -> IrSyntheticBodyKind.ENUM_VALUES
            ProtoSyntheticBodyKind.ENUM_VALUEOF -> IrSyntheticBodyKind.ENUM_VALUEOF
        }
        return IrSyntheticBodyImpl(start, end, kind)
    }

    fun deserializeStatement(proto: ProtoStatement): IrElement {
        val start = proto.coordinates.startOffset
        val end = proto.coordinates.endOffset
        val element = when (proto.statementCase) {
            StatementCase.BLOCK_BODY //proto.hasBlockBody()
            -> deserializeBlockBody(proto.blockBody, start, end)
            StatementCase.BRANCH //proto.hasBranch()
            -> deserializeBranch(proto.branch, start, end)
            StatementCase.CATCH //proto.hasCatch()
            -> deserializeCatch(proto.catch, start, end)
            StatementCase.DECLARATION // proto.hasDeclaration()
            -> deserializeDeclaration(proto.declaration)
            StatementCase.EXPRESSION // proto.hasExpression()
            -> deserializeExpression(proto.expression)
            StatementCase.SYNTHETIC_BODY // proto.hasSyntheticBody()
            -> deserializeSyntheticBody(proto.syntheticBody, start, end)
            else
            -> TODO("Statement deserialization not implemented: ${proto.statementCase}")
        }

        logger.log { "### Deserialized statement: ${ir2string(element)}" }

        return element
    }

    private fun deserializeBlock(proto: ProtoBlock, start: Int, end: Int, type: IrType): IrBlock {
        val statements = mutableListOf<IrStatement>()
        val statementProtos = proto.statementList
        val origin = if (proto.hasOrigin()) deserializeIrStatementOrigin(proto.origin) else null

        statementProtos.forEach {
            statements.add(deserializeStatement(it) as IrStatement)
        }

        return IrBlockImpl(start, end, type, origin, statements)
    }

    private fun deserializeMemberAccessCommon(access: IrMemberAccessExpression, proto: ProtoMemberAccessCommon) {

        proto.valueArgumentList.mapIndexed { i, arg ->
            if (arg.hasExpression()) {
                val expr = deserializeExpression(arg.expression)
                access.putValueArgument(i, expr)
            }
        }

        deserializeTypeArguments(proto.typeArguments).forEachIndexed { index, type ->
            access.putTypeArgument(index, type)
        }

        if (proto.hasDispatchReceiver()) {
            access.dispatchReceiver = deserializeExpression(proto.dispatchReceiver)
        }
        if (proto.hasExtensionReceiver()) {
            access.extensionReceiver = deserializeExpression(proto.extensionReceiver)
        }
    }

    private fun deserializeClassReference(
        proto: ProtoClassReference,
        start: Int,
        end: Int,
        type: IrType
    ): IrClassReference {
        val symbol = deserializeIrSymbol(proto.classSymbol) as IrClassifierSymbol
        val classType = deserializeIrType(proto.classType)
        /** TODO: [createClassifierSymbolForClassReference] is internal function */
        return IrClassReferenceImpl(start, end, type, symbol, classType)
    }

    private fun deserializeConstructorCall(proto: ProtoConstructorCall, start: Int, end: Int, type: IrType): IrConstructorCall {
        val symbol = deserializeIrSymbol(proto.symbol) as IrConstructorSymbol
        return IrConstructorCallImpl(
            start, end, type,
            symbol, typeArgumentsCount = proto.memberAccess.typeArguments.typeArgumentCount,
            constructorTypeArgumentsCount = proto.constructorTypeArgumentsCount,
            valueArgumentsCount = proto.memberAccess.valueArgumentCount
        ).also {
            deserializeMemberAccessCommon(it, proto.memberAccess)
        }
    }

    private fun deserializeCall(proto: ProtoCall, start: Int, end: Int, type: IrType): IrCall {
        val symbol = deserializeIrSymbol(proto.symbol) as IrFunctionSymbol

        val superSymbol = if (proto.hasSuper()) {
            deserializeIrSymbol(proto.`super`) as IrClassSymbol
        } else null

        val origin = if (proto.hasOrigin()) deserializeIrStatementOrigin(proto.origin) else null

        val call: IrCall =
            // TODO: implement the last three args here.
            IrCallImpl(
                start, end, type,
                symbol, proto.memberAccess.typeArguments.typeArgumentCount,
                proto.memberAccess.valueArgumentList.size,
                origin,
                superSymbol
            )
        deserializeMemberAccessCommon(call, proto.memberAccess)
        return call
    }

    private fun deserializeComposite(proto: ProtoComposite, start: Int, end: Int, type: IrType): IrComposite {
        val statements = mutableListOf<IrStatement>()
        val statementProtos = proto.statementList
        val origin = if (proto.hasOrigin()) deserializeIrStatementOrigin(proto.origin) else null

        statementProtos.forEach {
            statements.add(deserializeStatement(it) as IrStatement)
        }
        return IrCompositeImpl(start, end, type, origin, statements)
    }

    private fun deserializeDelegatingConstructorCall(
        proto: ProtoDelegatingConstructorCall,
        start: Int,
        end: Int
    ): IrDelegatingConstructorCall {
        val symbol = deserializeIrSymbol(proto.symbol) as IrConstructorSymbol
        val call = IrDelegatingConstructorCallImpl(
            start,
            end,
            builtIns.unitType,
            symbol,
            proto.memberAccess.typeArguments.typeArgumentCount,
            proto.memberAccess.valueArgumentList.size
        )

        deserializeMemberAccessCommon(call, proto.memberAccess)
        return call
    }


    private fun deserializeEnumConstructorCall(
        proto: ProtoEnumConstructorCall,
        start: Int,
        end: Int,
        type: IrType
    ): IrEnumConstructorCall {
        val symbol = deserializeIrSymbol(proto.symbol) as IrConstructorSymbol
        val call = IrEnumConstructorCallImpl(
            start,
            end,
            builtIns.unitType,
            symbol,
            proto.memberAccess.typeArguments.typeArgumentList.size,
            proto.memberAccess.valueArgumentList.size
        )
        deserializeMemberAccessCommon(call, proto.memberAccess)
        return call
    }

    private fun deserializeFunctionExpression(
        functionExpression: ProtoFunctionExpression,
        start: Int,
        end: Int,
        type: IrType
    ) =
        IrFunctionExpressionImpl(
            start, end, type,
            deserializeIrFunction(functionExpression.function),
            deserializeIrStatementOrigin(functionExpression.origin)
        )

    private fun deserializeFunctionReference(
        proto: ProtoFunctionReference,
        start: Int, end: Int, type: IrType
    ): IrFunctionReference {

        val symbol = deserializeIrSymbol(proto.symbol) as IrFunctionSymbol
        val origin = if (proto.hasOrigin()) deserializeIrStatementOrigin(proto.origin) else null
        val callable = IrFunctionReferenceImpl(
            start,
            end,
            type,
            symbol,
            proto.memberAccess.typeArguments.typeArgumentCount,
            proto.memberAccess.valueArgumentCount,
            origin
        )
        deserializeMemberAccessCommon(callable, proto.memberAccess)

        return callable
    }

    private fun deserializeGetClass(proto: ProtoGetClass, start: Int, end: Int, type: IrType): IrGetClass {
        val argument = deserializeExpression(proto.argument)
        return IrGetClassImpl(start, end, type, argument)
    }

    private fun deserializeGetField(proto: ProtoGetField, start: Int, end: Int, type: IrType): IrGetField {
        val access = proto.fieldAccess
        val symbol = deserializeIrSymbol(access.symbol) as IrFieldSymbol
        val origin = if (proto.hasOrigin()) deserializeIrStatementOrigin(proto.origin) else null

        val superQualifier = if (access.hasSuper()) {
            deserializeIrSymbol(access.symbol) as IrClassSymbol
        } else null
        val receiver = if (access.hasReceiver()) {
            deserializeExpression(access.receiver)
        } else null

        return IrGetFieldImpl(start, end, symbol, type, receiver, origin, superQualifier)
    }

    private fun deserializeGetValue(proto: ProtoGetValue, start: Int, end: Int, type: IrType): IrGetValue {
        val symbol = deserializeIrSymbol(proto.symbol) as IrValueSymbol
        val origin = if (proto.hasOrigin()) deserializeIrStatementOrigin(proto.origin) else null
        // TODO: origin!
        return IrGetValueImpl(start, end, type, symbol, origin)
    }

    private fun deserializeGetEnumValue(
        proto: ProtoGetEnumValue,
        start: Int,
        end: Int,
        type: IrType
    ): IrGetEnumValue {
        val symbol = deserializeIrSymbol(proto.symbol) as IrEnumEntrySymbol
        return IrGetEnumValueImpl(start, end, type, symbol)
    }

    private fun deserializeGetObject(
        proto: ProtoGetObject,
        start: Int,
        end: Int,
        type: IrType
    ): IrGetObjectValue {
        val symbol = deserializeIrSymbol(proto.symbol) as IrClassSymbol
        return IrGetObjectValueImpl(start, end, type, symbol)
    }

    private fun deserializeInstanceInitializerCall(
        proto: ProtoInstanceInitializerCall,
        start: Int,
        end: Int
    ): IrInstanceInitializerCall {
        val symbol = deserializeIrSymbol(proto.symbol) as IrClassSymbol
        return IrInstanceInitializerCallImpl(start, end, symbol, builtIns.unitType)
    }

    private fun deserializeIrLocalDelegatedPropertyReference(
        proto: ProtoLocalDelegatedPropertyReference,
        start: Int,
        end: Int,
        type: IrType
    ): IrLocalDelegatedPropertyReference {

        val delegate = deserializeIrSymbol(proto.delegate) as IrVariableSymbol
        val getter = deserializeIrSymbol(proto.getter) as IrSimpleFunctionSymbol
        val setter = if (proto.hasSetter()) deserializeIrSymbol(proto.setter) as IrSimpleFunctionSymbol else null
        val symbol = deserializeIrSymbol(proto.symbol) as IrLocalDelegatedPropertySymbol
        val origin = if (proto.hasOrigin()) deserializeIrStatementOrigin(proto.origin) else null

        return IrLocalDelegatedPropertyReferenceImpl(
            start, end, type,
            symbol,
            delegate,
            getter,
            setter,
            origin
        )
    }

    private fun deserializePropertyReference(
        proto: ProtoPropertyReference,
        start: Int, end: Int, type: IrType
    ): IrPropertyReference {

        val symbol = deserializeIrSymbol(proto.symbol) as IrPropertySymbol

        val field = if (proto.hasField()) deserializeIrSymbol(proto.field) as IrFieldSymbol else null
        val getter = if (proto.hasGetter()) deserializeIrSymbol(proto.getter) as IrSimpleFunctionSymbol else null
        val setter = if (proto.hasSetter()) deserializeIrSymbol(proto.setter) as IrSimpleFunctionSymbol else null
        val origin = if (proto.hasOrigin()) deserializeIrStatementOrigin(proto.origin) else null

        val callable = IrPropertyReferenceImpl(
            start, end, type,
            symbol,
            proto.memberAccess.typeArguments.typeArgumentCount,
            field,
            getter,
            setter,
            origin
        )
        deserializeMemberAccessCommon(callable, proto.memberAccess)
        return callable
    }

    private fun deserializeReturn(proto: ProtoReturn, start: Int, end: Int, type: IrType): IrReturn {
        val symbol = deserializeIrSymbol(proto.returnTarget) as IrReturnTargetSymbol
        val value = deserializeExpression(proto.value)
        return IrReturnImpl(start, end, builtIns.nothingType, symbol, value)
    }

    private fun deserializeSetField(proto: ProtoSetField, start: Int, end: Int): IrSetField {
        val access = proto.fieldAccess
        val symbol = deserializeIrSymbol(access.symbol) as IrFieldSymbol
        val superQualifier = if (access.hasSuper()) {
            deserializeIrSymbol(access.symbol) as IrClassSymbol
        } else null
        val receiver = if (access.hasReceiver()) {
            deserializeExpression(access.receiver)
        } else null
        val value = deserializeExpression(proto.value)
        val origin = if (proto.hasOrigin()) deserializeIrStatementOrigin(proto.origin) else null

        return IrSetFieldImpl(start, end, symbol, receiver, value, builtIns.unitType, origin, superQualifier)
    }

    private fun deserializeSetVariable(proto: ProtoSetVariable, start: Int, end: Int): IrSetVariable {
        val symbol = deserializeIrSymbol(proto.symbol) as IrVariableSymbol
        val value = deserializeExpression(proto.value)
        val origin = if (proto.hasOrigin()) deserializeIrStatementOrigin(proto.origin) else null
        return IrSetVariableImpl(start, end, builtIns.unitType, symbol, value, origin)
    }

    private fun deserializeSpreadElement(proto: ProtoSpreadElement): IrSpreadElement {
        val expression = deserializeExpression(proto.expression)
        return IrSpreadElementImpl(proto.coordinates.startOffset, proto.coordinates.endOffset, expression)
    }

    private fun deserializeStringConcat(
        proto: ProtoStringConcat,
        start: Int,
        end: Int,
        type: IrType
    ): IrStringConcatenation {
        val argumentProtos = proto.argumentList
        val arguments = mutableListOf<IrExpression>()

        argumentProtos.forEach {
            arguments.add(deserializeExpression(it))
        }
        return IrStringConcatenationImpl(start, end, type, arguments)
    }

    private fun deserializeThrow(proto: ProtoThrow, start: Int, end: Int, type: IrType): IrThrowImpl {
        return IrThrowImpl(start, end, builtIns.nothingType, deserializeExpression(proto.value))
    }

    private fun deserializeTry(proto: ProtoTry, start: Int, end: Int, type: IrType): IrTryImpl {
        val result = deserializeExpression(proto.result)
        val catches = mutableListOf<IrCatch>()
        proto.catchList.forEach {
            catches.add(deserializeStatement(it) as IrCatch)
        }
        val finallyExpression =
            if (proto.hasFinally()) deserializeExpression(proto.getFinally()) else null
        return IrTryImpl(start, end, type, result, catches, finallyExpression)
    }

    private fun deserializeTypeOperator(operator: ProtoTypeOperator) = when (operator) {
        ProtoTypeOperator.CAST ->
            IrTypeOperator.CAST
        ProtoTypeOperator.IMPLICIT_CAST ->
            IrTypeOperator.IMPLICIT_CAST
        ProtoTypeOperator.IMPLICIT_NOTNULL ->
            IrTypeOperator.IMPLICIT_NOTNULL
        ProtoTypeOperator.IMPLICIT_COERCION_TO_UNIT ->
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT
        ProtoTypeOperator.IMPLICIT_INTEGER_COERCION ->
            IrTypeOperator.IMPLICIT_INTEGER_COERCION
        ProtoTypeOperator.SAFE_CAST ->
            IrTypeOperator.SAFE_CAST
        ProtoTypeOperator.INSTANCEOF ->
            IrTypeOperator.INSTANCEOF
        ProtoTypeOperator.NOT_INSTANCEOF ->
            IrTypeOperator.NOT_INSTANCEOF
        ProtoTypeOperator.SAM_CONVERSION ->
            IrTypeOperator.SAM_CONVERSION
        ProtoTypeOperator.IMPLICIT_DYNAMIC_CAST ->
            IrTypeOperator.IMPLICIT_DYNAMIC_CAST
    }

    private fun deserializeTypeOp(proto: ProtoTypeOp, start: Int, end: Int, type: IrType): IrTypeOperatorCall {
        val operator = deserializeTypeOperator(proto.operator)
        val operand = deserializeIrType(proto.operand)//.brokenIr
        val argument = deserializeExpression(proto.argument)
        return IrTypeOperatorCallImpl(start, end, type, operator, operand).apply {
            this.argument = argument
        }
    }

    private fun deserializeVararg(proto: ProtoVararg, start: Int, end: Int, type: IrType): IrVararg {
        val elementType = deserializeIrType(proto.elementType)

        val elements = mutableListOf<IrVarargElement>()
        proto.elementList.forEach {
            elements.add(deserializeVarargElement(it))
        }
        return IrVarargImpl(start, end, type, elementType, elements)
    }

    private fun deserializeVarargElement(element: ProtoVarargElement): IrVarargElement {
        return when (element.varargElementCase) {
            VarargElementCase.EXPRESSION
            -> deserializeExpression(element.expression)
            VarargElementCase.SPREAD_ELEMENT
            -> deserializeSpreadElement(element.spreadElement)
            else
            -> TODO("Unexpected vararg element")
        }
    }

    private fun deserializeWhen(proto: ProtoWhen, start: Int, end: Int, type: IrType): IrWhen {
        val branches = mutableListOf<IrBranch>()
        val origin = if (proto.hasOrigin()) deserializeIrStatementOrigin(proto.origin) else null

        proto.branchList.forEach {
            branches.add(deserializeStatement(it) as IrBranch)
        }

        // TODO: provide some origin!
        return IrWhenImpl(start, end, type, origin, branches)
    }

    private fun deserializeLoop(proto: ProtoLoop, loop: IrLoopBase): IrLoopBase {
        val label = if (proto.hasLabel()) deserializeString(proto.label) else null
        val body = if (proto.hasBody()) deserializeExpression(proto.body) else null
        val condition = deserializeExpression(proto.condition)

        loop.label = label
        loop.condition = condition
        loop.body = body

        return loop
    }

    // we create the loop before deserializing the body, so that
    // IrBreak statements have something to put into 'loop' field.
    private fun deserializeDoWhile(proto: ProtoDoWhile, start: Int, end: Int, type: IrType) =
        deserializeLoop(proto.loop, deserializeLoopHeader(proto.loop.loopId) {
            val origin = if (proto.loop.hasOrigin()) deserializeIrStatementOrigin(proto.loop.origin) else null
            IrDoWhileLoopImpl(start, end, type, origin)
        })

    private fun deserializeWhile(proto: ProtoWhile, start: Int, end: Int, type: IrType) =
        deserializeLoop(proto.loop, deserializeLoopHeader(proto.loop.loopId) {
            val origin = if (proto.loop.hasOrigin()) deserializeIrStatementOrigin(proto.loop.origin) else null
            IrWhileLoopImpl(start, end, type, origin)
        })

    private fun deserializeDynamicMemberExpression(
        proto: ProtoDynamicMemberExpression,
        start: Int,
        end: Int,
        type: IrType
    ) =
        IrDynamicMemberExpressionImpl(
            start,
            end,
            type,
            deserializeString(proto.memberName),
            deserializeExpression(proto.receiver)
        )

    private fun deserializeDynamicOperatorExpression(
        proto: ProtoDynamicOperatorExpression,
        start: Int,
        end: Int,
        type: IrType
    ) =
        IrDynamicOperatorExpressionImpl(start, end, type, deserializeDynamicOperator(proto.operator)).apply {
            receiver = deserializeExpression(proto.receiver)
            proto.argumentList.mapTo(arguments) { deserializeExpression(it) }
        }

    private fun deserializeDynamicOperator(operator: ProtoDynamicOperatorExpression.IrDynamicOperator) =
        when (operator) {
            ProtoDynamicOperatorExpression.IrDynamicOperator.UNARY_PLUS -> IrDynamicOperator.UNARY_PLUS
            ProtoDynamicOperatorExpression.IrDynamicOperator.UNARY_MINUS -> IrDynamicOperator.UNARY_MINUS

            ProtoDynamicOperatorExpression.IrDynamicOperator.EXCL -> IrDynamicOperator.EXCL

            ProtoDynamicOperatorExpression.IrDynamicOperator.PREFIX_INCREMENT -> IrDynamicOperator.PREFIX_INCREMENT
            ProtoDynamicOperatorExpression.IrDynamicOperator.PREFIX_DECREMENT -> IrDynamicOperator.PREFIX_DECREMENT

            ProtoDynamicOperatorExpression.IrDynamicOperator.POSTFIX_INCREMENT -> IrDynamicOperator.POSTFIX_INCREMENT
            ProtoDynamicOperatorExpression.IrDynamicOperator.POSTFIX_DECREMENT -> IrDynamicOperator.POSTFIX_DECREMENT

            ProtoDynamicOperatorExpression.IrDynamicOperator.BINARY_PLUS -> IrDynamicOperator.BINARY_PLUS
            ProtoDynamicOperatorExpression.IrDynamicOperator.BINARY_MINUS -> IrDynamicOperator.BINARY_MINUS
            ProtoDynamicOperatorExpression.IrDynamicOperator.MUL -> IrDynamicOperator.MUL
            ProtoDynamicOperatorExpression.IrDynamicOperator.DIV -> IrDynamicOperator.DIV
            ProtoDynamicOperatorExpression.IrDynamicOperator.MOD -> IrDynamicOperator.MOD

            ProtoDynamicOperatorExpression.IrDynamicOperator.GT -> IrDynamicOperator.GT
            ProtoDynamicOperatorExpression.IrDynamicOperator.LT -> IrDynamicOperator.LT
            ProtoDynamicOperatorExpression.IrDynamicOperator.GE -> IrDynamicOperator.GE
            ProtoDynamicOperatorExpression.IrDynamicOperator.LE -> IrDynamicOperator.LE

            ProtoDynamicOperatorExpression.IrDynamicOperator.EQEQ -> IrDynamicOperator.EQEQ
            ProtoDynamicOperatorExpression.IrDynamicOperator.EXCLEQ -> IrDynamicOperator.EXCLEQ

            ProtoDynamicOperatorExpression.IrDynamicOperator.EQEQEQ -> IrDynamicOperator.EQEQEQ
            ProtoDynamicOperatorExpression.IrDynamicOperator.EXCLEQEQ -> IrDynamicOperator.EXCLEQEQ

            ProtoDynamicOperatorExpression.IrDynamicOperator.ANDAND -> IrDynamicOperator.ANDAND
            ProtoDynamicOperatorExpression.IrDynamicOperator.OROR -> IrDynamicOperator.OROR

            ProtoDynamicOperatorExpression.IrDynamicOperator.EQ -> IrDynamicOperator.EQ
            ProtoDynamicOperatorExpression.IrDynamicOperator.PLUSEQ -> IrDynamicOperator.PLUSEQ
            ProtoDynamicOperatorExpression.IrDynamicOperator.MINUSEQ -> IrDynamicOperator.MINUSEQ
            ProtoDynamicOperatorExpression.IrDynamicOperator.MULEQ -> IrDynamicOperator.MULEQ
            ProtoDynamicOperatorExpression.IrDynamicOperator.DIVEQ -> IrDynamicOperator.DIVEQ
            ProtoDynamicOperatorExpression.IrDynamicOperator.MODEQ -> IrDynamicOperator.MODEQ

            ProtoDynamicOperatorExpression.IrDynamicOperator.ARRAY_ACCESS -> IrDynamicOperator.ARRAY_ACCESS

            ProtoDynamicOperatorExpression.IrDynamicOperator.INVOKE -> IrDynamicOperator.INVOKE
        }

    private fun deserializeBreak(proto: ProtoBreak, start: Int, end: Int, type: IrType): IrBreak {
        val label = if (proto.hasLabel()) deserializeString(proto.label) else null
        val loopId = proto.loopId
        val loop = deserializeLoopHeader(loopId) { error("break clause before loop header") }
        val irBreak = IrBreakImpl(start, end, type, loop)
        irBreak.label = label

        return irBreak
    }

    private fun deserializeContinue(proto: ProtoContinue, start: Int, end: Int, type: IrType): IrContinue {
        val label = if (proto.hasLabel()) deserializeString(proto.label) else null
        val loopId = proto.loopId
        val loop = deserializeLoopHeader(loopId) { error("continue clause before loop header") }
        val irContinue = IrContinueImpl(start, end, type, loop)
        irContinue.label = label

        return irContinue
    }

    private fun deserializeConst(proto: ProtoConst, start: Int, end: Int, type: IrType): IrExpression =
        when (proto.valueCase!!) {
            NULL
            -> IrConstImpl.constNull(start, end, type)
            BOOLEAN
            -> IrConstImpl.boolean(start, end, type, proto.boolean)
            BYTE
            -> IrConstImpl.byte(start, end, type, proto.byte.toByte())
            CHAR
            -> IrConstImpl.char(start, end, type, proto.char.toChar())
            SHORT
            -> IrConstImpl.short(start, end, type, proto.short.toShort())
            INT
            -> IrConstImpl.int(start, end, type, proto.int)
            LONG
            -> IrConstImpl.long(start, end, type, proto.long)
            STRING
            -> IrConstImpl.string(start, end, type, deserializeString(proto.string))
            FLOAT
            -> IrConstImpl.float(start, end, type, proto.float)
            DOUBLE
            -> IrConstImpl.double(start, end, type, proto.double)
            VALUE_NOT_SET
            -> error("Const deserialization error: ${proto.valueCase} ")
        }

    private fun deserializeOperation(proto: ProtoOperation, start: Int, end: Int, type: IrType): IrExpression =
        when (proto.operationCase!!) {
            BLOCK -> deserializeBlock(proto.block, start, end, type)
            BREAK -> deserializeBreak(proto.`break`, start, end, type)
            CLASS_REFERENCE -> deserializeClassReference(proto.classReference, start, end, type)
            CALL -> deserializeCall(proto.call, start, end, type)
            COMPOSITE -> deserializeComposite(proto.composite, start, end, type)
            CONST -> deserializeConst(proto.const, start, end, type)
            CONTINUE -> deserializeContinue(proto.`continue`, start, end, type)
            DELEGATING_CONSTRUCTOR_CALL -> deserializeDelegatingConstructorCall(proto.delegatingConstructorCall, start, end)
            DO_WHILE -> deserializeDoWhile(proto.doWhile, start, end, type)
            ENUM_CONSTRUCTOR_CALL -> deserializeEnumConstructorCall(proto.enumConstructorCall, start, end, type)
            FUNCTION_REFERENCE -> deserializeFunctionReference(proto.functionReference, start, end, type)
            GET_ENUM_VALUE -> deserializeGetEnumValue(proto.getEnumValue, start, end, type)
            GET_CLASS -> deserializeGetClass(proto.getClass, start, end, type)
            GET_FIELD -> deserializeGetField(proto.getField, start, end, type)
            GET_OBJECT -> deserializeGetObject(proto.getObject, start, end, type)
            GET_VALUE -> deserializeGetValue(proto.getValue, start, end, type)
            LOCAL_DELEGATED_PROPERTY_REFERENCE -> deserializeIrLocalDelegatedPropertyReference(proto.localDelegatedPropertyReference, start, end, type)
            INSTANCE_INITIALIZER_CALL -> deserializeInstanceInitializerCall(proto.instanceInitializerCall, start, end)
            PROPERTY_REFERENCE -> deserializePropertyReference(proto.propertyReference, start, end, type)
            RETURN -> deserializeReturn(proto.`return`, start, end, type)
            SET_FIELD -> deserializeSetField(proto.setField, start, end)
            SET_VARIABLE -> deserializeSetVariable(proto.setVariable, start, end)
            STRING_CONCAT -> deserializeStringConcat(proto.stringConcat, start, end, type)
            THROW -> deserializeThrow(proto.`throw`, start, end, type)
            TRY -> deserializeTry(proto.`try`, start, end, type)
            TYPE_OP -> deserializeTypeOp(proto.typeOp, start, end, type)
            VARARG -> deserializeVararg(proto.vararg, start, end, type)
            WHEN -> deserializeWhen(proto.`when`, start, end, type)
            WHILE -> deserializeWhile(proto.`while`, start, end, type)
            DYNAMIC_MEMBER -> deserializeDynamicMemberExpression(proto.dynamicMember, start, end, type)
            DYNAMIC_OPERATOR -> deserializeDynamicOperatorExpression(proto.dynamicOperator, start, end, type)
            CONSTRUCTOR_CALL -> deserializeConstructorCall(proto.constructorCall, start, end, type)
            FUNCTION_EXPRESSION -> deserializeFunctionExpression(proto.functionExpression, start, end, type)
            OPERATION_NOT_SET -> error("Expression deserialization not implemented: ${proto.operationCase}")
        }

    fun deserializeExpression(proto: ProtoExpression): IrExpression {
        val start = proto.coordinates.startOffset
        val end = proto.coordinates.endOffset
        val type = deserializeIrType(proto.type)
        val operation = proto.operation
        val expression = deserializeOperation(operation, start, end, type)

        logger.log { "### Deserialized expression: ${ir2string(expression)} ir_type=$type" }
        return expression
    }

    private inline fun <T : IrDeclarationParent, R> usingParent(parent: T, block: (T) -> R): R {
        parentsStack.push(parent)
        try {
            return block(parent)
        } finally {
            parentsStack.pop()
        }
    }

    private inline fun <T : IrDeclarationParent> T.usingParent(block: T.() -> Unit): T =
        this.apply { usingParent(this) { block(it) } }

    private inline fun <T> withDeserializedIrDeclarationBase(
        proto: ProtoDeclarationBase,
        block: (IrSymbol, Int, Int, IrDeclarationOrigin) -> T
    ): T where T : IrDeclaration, T : IrSymbolOwner {
        val result = block(
            deserializeIrSymbol(proto.symbol),
            proto.coordinates.startOffset, proto.coordinates.endOffset,
            deserializeIrDeclarationOrigin(proto.origin)
        )
        result.annotations.addAll(deserializeAnnotations(proto.annotationList))
        result.parent = parentsStack.peek()!!
        return result
    }

    private fun deserializeIrTypeParameter(proto: ProtoTypeParameter) =
        withDeserializedIrDeclarationBase(proto.base) { symbol, startOffset, endOffset, origin ->
            val name = deserializeName(proto.name)
            val variance = deserializeIrTypeVariance(proto.variance)

            val descriptor = (symbol as IrTypeParameterSymbol).descriptor
            if (descriptor is DeserializedTypeParameterDescriptor && descriptor.containingDeclaration is PropertyDescriptor && symbol.isBound) {
                // TODO: Get rid of once new properties are implemented
                IrTypeParameterImpl(startOffset, endOffset, origin, IrTypeParameterSymbolImpl(descriptor), name, proto.index, proto.isReified, variance)
            } else {
                symbolTable.declareGlobalTypeParameter(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irrelevantOrigin, descriptor) {
                    IrTypeParameterImpl(startOffset, endOffset, origin, it, name, proto.index, proto.isReified, variance)
                }
            }.apply {
                proto.superTypeList.mapTo(superTypes) { deserializeIrType(it) }

                (descriptor as? WrappedTypeParameterDescriptor)?.bind(this)
            }
        }

    private fun deserializeClassKind(kind: ProtoClassKind) = when (kind) {
        ProtoClassKind.CLASS -> ClassKind.CLASS
        ProtoClassKind.INTERFACE -> ClassKind.INTERFACE
        ProtoClassKind.ENUM_CLASS -> ClassKind.ENUM_CLASS
        ProtoClassKind.ENUM_ENTRY -> ClassKind.ENUM_ENTRY
        ProtoClassKind.ANNOTATION_CLASS -> ClassKind.ANNOTATION_CLASS
        ProtoClassKind.OBJECT -> ClassKind.OBJECT
    }

    private fun deserializeIrValueParameter(proto: ProtoValueParameter) =
        withDeserializedIrDeclarationBase(proto.base) { symbol, startOffset, endOffset, origin ->
            IrValueParameterImpl(
                startOffset, endOffset, origin,
                symbol as IrValueParameterSymbol,
                deserializeName(proto.name),
                proto.index,
                deserializeIrType(proto.type),
                if (proto.hasVarargElementType()) deserializeIrType(proto.varargElementType) else null,
                proto.isCrossinline,
                proto.isNoinline
            ).apply {
                if (proto.hasDefaultValue())
                    defaultValue = IrExpressionBodyImpl(deserializeExpressionBody(proto.defaultValue))

                (descriptor as? WrappedValueParameterDescriptor)?.bind(this)
                (descriptor as? WrappedReceiverParameterDescriptor)?.bind(this)
            }
        }

    private fun deserializeIrClass(proto: ProtoClass) =
        withDeserializedIrDeclarationBase(proto.base) { symbol, startOffset, endOffset, origin ->
            val modality = deserializeModality(proto.modality)

            symbolTable.declareClass(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, irrelevantOrigin,
                (symbol as IrClassSymbol).descriptor, modality
            ) {
                IrClassImpl(
                    startOffset, endOffset, origin,
                    it,
                    deserializeName(proto.name),
                    deserializeClassKind(proto.kind),
                    deserializeVisibility(proto.visibility),
                    modality,
                    isCompanion = proto.isCompanion,
                    isInner = proto.isInner,
                    isData = proto.isData,
                    isExternal = proto.isExternal,
                    isInline = proto.isInline,
                    isExpect = proto.isExpect
                )
            }.usingParent {
                proto.declarationContainer.declarationList.mapTo(declarations) { deserializeDeclaration(it) }

                thisReceiver = deserializeIrValueParameter(proto.thisReceiver)

                proto.typeParameters.typeParameterList.mapTo(typeParameters) { deserializeIrTypeParameter(it) }

                proto.superTypeList.mapTo(superTypes) { deserializeIrType(it) }

                (descriptor as? WrappedClassDescriptor)?.bind(this)
            }
        }

    private fun deserializeIrTypeAlias(proto: ProtoTypeAlias) =
        withDeserializedIrDeclarationBase(proto.base) { symbol, startOffset, endOffset, origin ->
            symbolTable.declareTypeAlias((symbol as IrTypeAliasSymbol).descriptor) {
                IrTypeAliasImpl(
                    startOffset, endOffset,
                    it,
                    deserializeName(proto.name),
                    deserializeVisibility(proto.visibility),
                    deserializeIrType(proto.expandedType),
                    proto.isActual,
                    origin
                )
            }.usingParent {
                proto.typeParameters.typeParameterList.mapTo(typeParameters) {
                    deserializeIrTypeParameter(it)
                }

                (descriptor as? WrappedTypeAliasDescriptor)?.bind(this)
            }
        }

    private inline fun <T : IrFunction> withDeserializedIrFunctionBase(
        proto: ProtoFunctionBase,
        block: (IrFunctionSymbol, Int, Int, IrDeclarationOrigin) -> T
    ) = withDeserializedIrDeclarationBase(proto.base) { symbol, startOffset, endOffset, origin ->
        block(symbol as IrFunctionSymbol, startOffset, endOffset, origin).usingParent {
            proto.typeParameters.typeParameterList.mapTo(typeParameters) { deserializeIrTypeParameter(it) }
            proto.valueParameterList.mapTo(valueParameters) { deserializeIrValueParameter(it) }
            if (proto.hasDispatchReceiver())
                dispatchReceiverParameter = deserializeIrValueParameter(proto.dispatchReceiver)
            if (proto.hasExtensionReceiver())
                extensionReceiverParameter = deserializeIrValueParameter(proto.extensionReceiver)
            if (proto.hasBody()) {
                body = deserializeStatementBody(proto.body) as IrBody
            }
        }
    }

    private fun deserializeIrFunction(proto: ProtoFunction) =
        withDeserializedIrFunctionBase(proto.base) { symbol, startOffset, endOffset, origin ->
            logger.log { "### deserializing IrFunction ${proto.base.name}" }

            symbolTable.declareSimpleFunction(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, irrelevantOrigin,
                symbol.descriptor
            ) {
                IrFunctionImpl(
                    startOffset, endOffset, origin,
                    it,
                    deserializeName(proto.base.name),
                    deserializeVisibility(proto.base.visibility),
                    deserializeModality(proto.modality),
                    deserializeIrType(proto.base.returnType),
                    isInline = proto.base.isInline,
                    isExternal = proto.base.isExternal,
                    isTailrec = proto.isTailrec,
                    isSuspend = proto.isSuspend,
                    isExpect = proto.base.isExpect,
                    isFakeOverride = proto.isFakeOverride,
                    isOperator = proto.isOperator
                )
            }.apply {
                proto.overriddenList.mapTo(overriddenSymbols) { deserializeIrSymbol(it) as IrSimpleFunctionSymbol }

                (descriptor as? WrappedSimpleFunctionDescriptor)?.bind(this)
            }
        }

    private fun deserializeIrVariable(proto: ProtoVariable) =
        withDeserializedIrDeclarationBase(proto.base) { symbol, startOffset, endOffset, origin ->
            IrVariableImpl(
                startOffset, endOffset, origin,
                symbol as IrVariableSymbol,
                deserializeName(proto.name),
                deserializeIrType(proto.type),
                proto.isVar,
                proto.isConst,
                proto.isLateinit
            ).apply {
                if (proto.hasInitializer())
                    initializer = deserializeExpression(proto.initializer)

                (descriptor as? WrappedVariableDescriptor)?.bind(this)
            }
        }

    private fun deserializeIrEnumEntry(proto: ProtoEnumEntry): IrEnumEntry =
        withDeserializedIrDeclarationBase(proto.base) { symbol, startOffset, endOffset, origin ->
            symbolTable.declareEnumEntry(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                irrelevantOrigin,
                (symbol as IrEnumEntrySymbol).descriptor
            ) {
                IrEnumEntryImpl(startOffset, endOffset, origin, it, deserializeName(proto.name))
            }.apply {
                if (proto.hasCorrespondingClass())
                    correspondingClass = deserializeIrClass(proto.correspondingClass)
                if (proto.hasInitializer())
                    initializerExpression = deserializeExpressionBody(proto.initializer)

                (descriptor as? WrappedEnumEntryDescriptor)?.bind(this)
            }
        }

    private fun deserializeIrAnonymousInit(proto: ProtoAnonymousInit) =
        withDeserializedIrDeclarationBase(proto.base) { symbol, startOffset, endOffset, origin ->
            IrAnonymousInitializerImpl(startOffset, endOffset, origin, symbol as IrAnonymousInitializerSymbol).apply {
//                body = deserializeBlockBody(proto.body.blockBody, startOffset, endOffset)
                body = deserializeStatementBody(proto.body) as IrBlockBody

                (descriptor as? WrappedClassDescriptor)?.bind(parentsStack.peek() as IrClass)
            }
        }

    private fun deserializeVisibility(value: ProtoVisibility): Visibility { // TODO: switch to enum
        return when (deserializeString(value.name)) {
            "public" -> Visibilities.PUBLIC
            "private" -> Visibilities.PRIVATE
            "private_to_this" -> Visibilities.PRIVATE_TO_THIS
            "protected" -> Visibilities.PROTECTED
            "internal" -> Visibilities.INTERNAL
            "invisible_fake" -> Visibilities.INVISIBLE_FAKE // TODO: eventually we should not serialize fake overrides, so this will be gone.
            "local" -> Visibilities.LOCAL
            else -> error("Unexpected visibility value: $value")
        }
    }

    private fun deserializeIrConstructor(proto: ProtoConstructor) =
        withDeserializedIrFunctionBase(proto.base) { symbol, startOffset, endOffset, origin ->
            symbolTable.declareConstructor(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, irrelevantOrigin,
                (symbol as IrConstructorSymbol).descriptor
            ) {
                IrConstructorImpl(
                    startOffset, endOffset, origin,
                    it,
                    deserializeName(proto.base.name),
                    deserializeVisibility(proto.base.visibility),
                    deserializeIrType(proto.base.returnType),
                    isInline = proto.base.isInline,
                    isExternal = proto.base.isExternal,
                    isPrimary = proto.isPrimary,
                    isExpect = proto.base.isExpect
                )
            }.apply {
                (descriptor as? WrappedClassConstructorDescriptor)?.bind(this)
            }
        }

    private fun deserializeIrField(proto: ProtoField) =
        withDeserializedIrDeclarationBase(proto.base) { symbol, startOffset, endOffset, origin ->
            val type = deserializeIrType(proto.type)

            symbolTable.declareField(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, irrelevantOrigin,
                (symbol as IrFieldSymbol).descriptor,
                type
            ) {
                IrFieldImpl(
                    startOffset, endOffset, origin,
                    it,
                    deserializeName(proto.name),
                    type,
                    deserializeVisibility(proto.visibility),
                    isFinal = proto.isFinal,
                    isExternal = proto.isExternal,
                    isStatic = proto.isStatic,
                    isFakeOverride = proto.isFakeOverride
                )
            }.usingParent {
                if (proto.hasInitializer())
                    initializer = IrExpressionBodyImpl(deserializeExpressionBody(proto.initializer))

                (descriptor as? WrappedFieldDescriptor)?.bind(this)
            }
        }

    private fun deserializeModality(modality: ProtoModalityKind) = when (modality) {
        ProtoModalityKind.OPEN_MODALITY -> Modality.OPEN
        ProtoModalityKind.SEALED_MODALITY -> Modality.SEALED
        ProtoModalityKind.FINAL_MODALITY -> Modality.FINAL
        ProtoModalityKind.ABSTRACT_MODALITY -> Modality.ABSTRACT
    }

    private fun deserializeIrLocalDelegatedProperty(proto: ProtoLocalDelegatedProperty) =
        withDeserializedIrDeclarationBase(proto.base) { symbol, startOffset, endOffset, origin ->
            IrLocalDelegatedPropertyImpl(
                startOffset, endOffset, origin,
                symbol as IrLocalDelegatedPropertySymbol,
                deserializeName(proto.name),
                deserializeIrType(proto.type),
                proto.isVar
            ).apply {
                delegate = deserializeIrVariable(proto.delegate)
                getter = deserializeIrFunction(proto.getter)
                if (proto.hasSetter())
                    setter = deserializeIrFunction(proto.setter)

                (descriptor as? WrappedVariableDescriptorWithAccessor)?.bind(this)
            }
        }

    private fun deserializeIrProperty(proto: ProtoProperty) =
        withDeserializedIrDeclarationBase(proto.base) { symbol, startOffset, endOffset, origin ->
            symbolTable.declareProperty(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, irrelevantOrigin,
                (symbol as IrPropertySymbol).descriptor, proto.isDelegated
            ) {
                IrPropertyImpl(
                    startOffset, endOffset, origin,
                    it,
                    deserializeName(proto.name),
                    deserializeVisibility(proto.visibility),
                    deserializeModality(proto.modality),
                    isVar = proto.isVar,
                    isConst = proto.isConst,
                    isLateinit = proto.isLateinit,
                    isDelegated = proto.isDelegated,
                    isExpect = proto.isExpect,
                    isExternal = proto.isExternal,
                    isFakeOverride = proto.isFakeOverride
                )
            }.apply {
                if (proto.hasGetter()) {
                    getter = deserializeIrFunction(proto.getter).also {
                        it.correspondingPropertySymbol = symbol
                    }
                }
                if (proto.hasSetter()) {
                    setter = deserializeIrFunction(proto.setter).also {
                        it.correspondingPropertySymbol = symbol
                    }
                }
                if (proto.hasBackingField()) {
                    backingField = deserializeIrField(proto.backingField).also {
                        // A property symbol and its field symbol share the same descriptor.
                        // Unfortunately symbol deserialization doesn't know anything about that.
                        // So we can end up with two wrapped property descriptors for property and its field.
                        // In that case we need to bind the field's one here.
                        if (descriptor != it.descriptor)
                            (it.descriptor as? WrappedPropertyDescriptor)?.bind(this)
                        it.correspondingPropertySymbol = symbol
                    }
                }

                (descriptor as? WrappedPropertyDescriptor)?.bind(this)
            }
        }

    private val allKnownDeclarationOrigins =
        IrDeclarationOrigin::class.nestedClasses.toList() + DeclarationFactory.FIELD_FOR_OUTER_THIS::class

    private val declarationOriginIndex =
        allKnownDeclarationOrigins.map { it.objectInstance as IrDeclarationOriginImpl }.associateBy { it.name }


    private fun deserializeIrDeclarationOrigin(proto: ProtoDeclarationOrigin): IrDeclarationOriginImpl {
        val originName = deserializeString(proto.custom)
        return declarationOriginIndex[originName] ?: object : IrDeclarationOriginImpl(originName) {}
    }

    private val allKnownStatementOrigins =
        IrStatementOrigin::class.nestedClasses.toList()
    private val statementOriginIndex =
        allKnownStatementOrigins.map { it.objectInstance as? IrStatementOriginImpl }.filterNotNull().associateBy { it.debugName }

    fun deserializeIrStatementOrigin(proto: ProtoStatementOrigin): IrStatementOrigin {
        return deserializeString(proto.name).let {
            val componentPrefix = "COMPONENT_"
            when {
                it.startsWith(componentPrefix) -> {
                    IrStatementOrigin.COMPONENT_N.withIndex(it.removePrefix(componentPrefix).toInt())
                }
                else -> statementOriginIndex[it] ?: error("Unexpected statement origin: $it")
            }
        }
    }

    private fun deserializeDeclaration(proto: ProtoDeclaration): IrDeclaration {
        val declaration: IrDeclaration = when (proto.declaratorCase!!) {
            IR_ANONYMOUS_INIT -> deserializeIrAnonymousInit(proto.irAnonymousInit)
            IR_CONSTRUCTOR -> deserializeIrConstructor(proto.irConstructor)
            IR_FIELD -> deserializeIrField(proto.irField)
            IR_CLASS -> deserializeIrClass(proto.irClass)
            IR_FUNCTION -> deserializeIrFunction(proto.irFunction)
            IR_PROPERTY -> deserializeIrProperty(proto.irProperty)
            IR_TYPE_PARAMETER -> deserializeIrTypeParameter(proto.irTypeParameter)
            IR_VARIABLE -> deserializeIrVariable(proto.irVariable)
            IR_VALUE_PARAMETER -> deserializeIrValueParameter(proto.irValueParameter)
            IR_ENUM_ENTRY -> deserializeIrEnumEntry(proto.irEnumEntry)
            IR_LOCAL_DELEGATED_PROPERTY -> deserializeIrLocalDelegatedProperty(proto.irLocalDelegatedProperty)
            IR_TYPE_ALIAS -> deserializeIrTypeAlias(proto.irTypeAlias)
            DECLARATOR_NOT_SET -> error("Declaration deserialization not implemented: ${proto.declaratorCase}")
        }

        logger.log { "### Deserialized declaration: ${declaration.descriptor} -> ${ir2string(declaration)}" }

        return declaration
    }

    fun deserializeDeclaration(proto: ProtoDeclaration, parent: IrDeclarationParent) =
        usingParent(parent) {
            deserializeDeclaration(proto)
        }
}

val irrelevantOrigin = object : IrDeclarationOriginImpl("irrelevant") {}
