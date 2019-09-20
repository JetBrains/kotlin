/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.nextgen

class SimpleIrProtoReader(source: ByteArray) : ProtoReader(source) {
    fun createDescriptorReference(packageFqName : Any, classFqName : Any, name : Any, uniqId : Any?, isGetter : Boolean?, isSetter : Boolean?, isBackingField : Boolean?, isFakeOverride : Boolean?, isDefaultConstructor : Boolean?, isEnumEntry : Boolean?, isEnumSpecial : Boolean?, isTypeParameter : Boolean?): Any = arrayOf<Any?>(packageFqName, classFqName, name, uniqId, isGetter, isSetter, isBackingField, isFakeOverride, isDefaultConstructor, isEnumEntry, isEnumSpecial, isTypeParameter)

    fun createUniqId(index : Long, isLocal : Boolean): Any = arrayOf<Any?>(index, isLocal)

    fun createVisibility(name : Any): Any = arrayOf<Any?>(name)

    fun createIrStatementOrigin(name : Any): Any = arrayOf<Any?>(name)

    fun createKnownOrigin(index: Int): Any = index

    fun createIrDeclarationOrigin_origin(oneOfOrigin : Any): Any = arrayOf<Any?>(oneOfOrigin)
    fun createIrDeclarationOrigin_custom(oneOfCustom : Any): Any = arrayOf<Any?>(oneOfCustom)

    fun createIrDataIndex(index : Int): Any = arrayOf<Any?>(index)

    fun createFqName(segment : List<Any>): Any = arrayOf<Any?>(segment)

    fun createIrDeclarationContainer(declaration : List<Any>): Any = arrayOf<Any?>(declaration)

    fun createFileEntry(name : String, lineStartOffsets : List<Int>): Any = arrayOf<Any?>(name, lineStartOffsets)

    fun createIrFile(declarationId : List<Any>, fileEntry : Any, fqName : Any, annotations : Any, explicitlyExportedToCompiler : List<Any>): Any = arrayOf<Any?>(declarationId, fileEntry, fqName, annotations, explicitlyExportedToCompiler)

    fun createStringTable(strings : List<String>): Any = arrayOf<Any?>(strings)

    fun createIrSymbolKind(index: Int): Any = index

    fun createIrSymbolData(kind : Any, uniqId : Any, topLevelUniqId : Any, fqname : Any?, descriptorReference : Any?): Any = arrayOf<Any?>(kind, uniqId, topLevelUniqId, fqname, descriptorReference)

    fun createIrSymbolTable(symbols : List<Any>): Any = arrayOf<Any?>(symbols)

    fun createIrTypeVariance(index: Int): Any = index

    fun createAnnotations(annotation : List<Any>): Any = arrayOf<Any?>(annotation)

    fun createTypeArguments(typeArgument : List<Any>): Any = arrayOf<Any?>(typeArgument)

    fun createIrStarProjection(void : Boolean?): Any = arrayOf<Any?>(void)

    fun createIrTypeProjection(variance : Any, type_ : Any): Any = arrayOf<Any?>(variance, type_)

    fun createIrTypeArgument_star(oneOfStar : Any): Any = arrayOf<Any?>(oneOfStar)
    fun createIrTypeArgument_type_(oneOfType : Any): Any = arrayOf<Any?>(oneOfType)

    fun createIrSimpleType(annotations : Any, classifier : Any, hasQuestionMark : Boolean, argument : List<Any>, abbreviation : Any?): Any = arrayOf<Any?>(annotations, classifier, hasQuestionMark, argument, abbreviation)

    fun createIrTypeAbbreviation(annotations : Any, typeAlias : Any, hasQuestionMark : Boolean, argument : List<Any>): Any = arrayOf<Any?>(annotations, typeAlias, hasQuestionMark, argument)

    fun createIrDynamicType(annotations : Any): Any = arrayOf<Any?>(annotations)

    fun createIrErrorType(annotations : Any): Any = arrayOf<Any?>(annotations)

    fun createIrType_simple(oneOfSimple : Any): Any = arrayOf<Any?>(oneOfSimple)
    fun createIrType_dynamic(oneOfDynamic : Any): Any = arrayOf<Any?>(oneOfDynamic)
    fun createIrType_error(oneOfError : Any): Any = arrayOf<Any?>(oneOfError)

    fun createIrTypeTable(types : List<Any>): Any = arrayOf<Any?>(types)

    fun createIrBreak(loopId : Int, label : Any?): Any = arrayOf<Any?>(loopId, label)

    fun createIrBlock(origin : Any?, statement : List<Any>): Any = arrayOf<Any?>(origin, statement)

    fun createIrCall(symbol : Any, memberAccessDispatchReceiver : Any?, memberAccessExtensionReceiver : Any?, memberAccessValueArgument : List<Any>, memberAccessTypeArguments : Any, super_ : Any?, origin : Any?): Any = arrayOf<Any?>(symbol, memberAccessDispatchReceiver, memberAccessExtensionReceiver, memberAccessValueArgument, memberAccessTypeArguments, super_, origin)

    fun createIrConstructorCall(symbol : Any, constructorTypeArgumentsCount : Int, memberAccessDispatchReceiver : Any?, memberAccessExtensionReceiver : Any?, memberAccessValueArgument : List<Any>, memberAccessTypeArguments : Any): Any = arrayOf<Any?>(symbol, constructorTypeArgumentsCount, memberAccessDispatchReceiver, memberAccessExtensionReceiver, memberAccessValueArgument, memberAccessTypeArguments)

    fun createIrFunctionReference(symbol : Any, origin : Any?, memberAccessDispatchReceiver : Any?, memberAccessExtensionReceiver : Any?, memberAccessValueArgument : List<Any>, memberAccessTypeArguments : Any): Any = arrayOf<Any?>(symbol, origin, memberAccessDispatchReceiver, memberAccessExtensionReceiver, memberAccessValueArgument, memberAccessTypeArguments)

    fun createIrLocalDelegatedPropertyReference(delegate : Any, getter : Any?, setter : Any?, symbol : Any, origin : Any?): Any = arrayOf<Any?>(delegate, getter, setter, symbol, origin)

    fun createIrPropertyReference(field : Any?, getter : Any?, setter : Any?, origin : Any?, memberAccessDispatchReceiver : Any?, memberAccessExtensionReceiver : Any?, memberAccessValueArgument : List<Any>, memberAccessTypeArguments : Any, symbol : Any): Any = arrayOf<Any?>(field, getter, setter, origin, memberAccessDispatchReceiver, memberAccessExtensionReceiver, memberAccessValueArgument, memberAccessTypeArguments, symbol)

    fun createIrComposite(statement : List<Any>, origin : Any?): Any = arrayOf<Any?>(statement, origin)

    fun createIrClassReference(classSymbol : Any, classType : Any): Any = arrayOf<Any?>(classSymbol, classType)

    fun createIrConst_null_(oneOfNull : Boolean): Any = arrayOf<Any?>(oneOfNull)
    fun createIrConst_boolean(oneOfBoolean : Boolean): Any = arrayOf<Any?>(oneOfBoolean)
    fun createIrConst_char(oneOfChar : Int): Any = arrayOf<Any?>(oneOfChar)
    fun createIrConst_byte(oneOfByte : Int): Any = arrayOf<Any?>(oneOfByte)
    fun createIrConst_short(oneOfShort : Int): Any = arrayOf<Any?>(oneOfShort)
    fun createIrConst_int(oneOfInt : Int): Any = arrayOf<Any?>(oneOfInt)
    fun createIrConst_long(oneOfLong : Long): Any = arrayOf<Any?>(oneOfLong)
    fun createIrConst_float(oneOfFloat : Float): Any = arrayOf<Any?>(oneOfFloat)
    fun createIrConst_double(oneOfDouble : Double): Any = arrayOf<Any?>(oneOfDouble)
    fun createIrConst_string(oneOfString : Any): Any = arrayOf<Any?>(oneOfString)

    fun createIrContinue(loopId : Int, label : Any?): Any = arrayOf<Any?>(loopId, label)

    fun createIrDelegatingConstructorCall(symbol : Any, memberAccessDispatchReceiver : Any?, memberAccessExtensionReceiver : Any?, memberAccessValueArgument : List<Any>, memberAccessTypeArguments : Any): Any = arrayOf<Any?>(symbol, memberAccessDispatchReceiver, memberAccessExtensionReceiver, memberAccessValueArgument, memberAccessTypeArguments)

    fun createIrDoWhile(loopLoopId : Int, loopCondition : Any, loopLabel : Any?, loopOrigin : Any?): Any = arrayOf<Any?>(loopLoopId, loopCondition, loopLabel, loopOrigin)
    fun createIrDoWhile1(partial: Any, loopBody : Any?): Any = arrayOf<Any?>(loopBody)

    fun createIrEnumConstructorCall(symbol : Any, memberAccessDispatchReceiver : Any?, memberAccessExtensionReceiver : Any?, memberAccessValueArgument : List<Any>, memberAccessTypeArguments : Any): Any = arrayOf<Any?>(symbol, memberAccessDispatchReceiver, memberAccessExtensionReceiver, memberAccessValueArgument, memberAccessTypeArguments)

    fun createIrGetClass(argument : Any): Any = arrayOf<Any?>(argument)

    fun createIrGetEnumValue(symbol : Any): Any = arrayOf<Any?>(symbol)

    fun createIrGetField(fieldAccessSymbol : Any, fieldAccessSuper : Any?, fieldAccessReceiver : Any?, origin : Any?): Any = arrayOf<Any?>(fieldAccessSymbol, fieldAccessSuper, fieldAccessReceiver, origin)

    fun createIrGetValue(symbol : Any, origin : Any?): Any = arrayOf<Any?>(symbol, origin)

    fun createIrGetObject(symbol : Any): Any = arrayOf<Any?>(symbol)

    fun createIrInstanceInitializerCall(symbol : Any): Any = arrayOf<Any?>(symbol)

    fun createIrReturn(returnTarget : Any, value : Any): Any = arrayOf<Any?>(returnTarget, value)

    fun createIrSetField(fieldAccessSymbol : Any, fieldAccessSuper : Any?, fieldAccessReceiver : Any?, value : Any, origin : Any?): Any = arrayOf<Any?>(fieldAccessSymbol, fieldAccessSuper, fieldAccessReceiver, value, origin)

    fun createIrSetVariable(symbol : Any, value : Any, origin : Any?): Any = arrayOf<Any?>(symbol, value, origin)

    fun createIrSpreadElement(expression : Any, coordinatesStartOffset : Int, coordinatesEndOffset : Int): Any = arrayOf<Any?>(expression, coordinatesStartOffset, coordinatesEndOffset)

    fun createIrStringConcat(argument : List<Any>): Any = arrayOf<Any?>(argument)

    fun createIrThrow(value : Any): Any = arrayOf<Any?>(value)

    fun createIrTry(result : Any, catch : List<Any>, finally : Any?): Any = arrayOf<Any?>(result, catch, finally)

    fun createIrTypeOp(operator : Any, operand : Any, argument : Any): Any = arrayOf<Any?>(operator, operand, argument)

    fun createIrVararg(elementType : Any, element : List<Any>): Any = arrayOf<Any?>(elementType, element)

    fun createIrVarargElement_expression(oneOfExpression : Any): Any = arrayOf<Any?>(oneOfExpression)
    fun createIrVarargElement_spreadElement(oneOfSpreadElement : Any): Any = arrayOf<Any?>(oneOfSpreadElement)

    fun createIrWhen(branch : List<Any>, origin : Any?): Any = arrayOf<Any?>(branch, origin)

    fun createIrWhile(loopLoopId : Int, loopCondition : Any, loopLabel : Any?, loopOrigin : Any?): Any = arrayOf<Any?>(loopLoopId, loopCondition, loopLabel, loopOrigin)
    fun createIrWhile1(partial: Any, loopBody : Any?): Any = arrayOf<Any?>(loopBody)

    fun createIrFunctionExpression(function : Any, origin : Any): Any = arrayOf<Any?>(function, origin)

    fun createIrDynamicMemberExpression(memberName : Any, receiver : Any): Any = arrayOf<Any?>(memberName, receiver)

    fun createIrDynamicOperator(index: Int): Any = index

    fun createIrDynamicOperatorExpression(operator : Any, receiver : Any, argument : List<Any>): Any = arrayOf<Any?>(operator, receiver, argument)

    fun createIrOperation_block(oneOfBlock : Any): Any = arrayOf<Any?>(oneOfBlock)
    fun createIrOperation_break_(oneOfBreak : Any): Any = arrayOf<Any?>(oneOfBreak)
    fun createIrOperation_call(oneOfCall : Any): Any = arrayOf<Any?>(oneOfCall)
    fun createIrOperation_classReference(oneOfClassReference : Any): Any = arrayOf<Any?>(oneOfClassReference)
    fun createIrOperation_composite(oneOfComposite : Any): Any = arrayOf<Any?>(oneOfComposite)
    fun createIrOperation_const(oneOfConst : Any): Any = arrayOf<Any?>(oneOfConst)
    fun createIrOperation_continue_(oneOfContinue : Any): Any = arrayOf<Any?>(oneOfContinue)
    fun createIrOperation_delegatingConstructorCall(oneOfDelegatingConstructorCall : Any): Any = arrayOf<Any?>(oneOfDelegatingConstructorCall)
    fun createIrOperation_doWhile(oneOfDoWhile : Any): Any = arrayOf<Any?>(oneOfDoWhile)
    fun createIrOperation_enumConstructorCall(oneOfEnumConstructorCall : Any): Any = arrayOf<Any?>(oneOfEnumConstructorCall)
    fun createIrOperation_functionReference(oneOfFunctionReference : Any): Any = arrayOf<Any?>(oneOfFunctionReference)
    fun createIrOperation_getClass(oneOfGetClass : Any): Any = arrayOf<Any?>(oneOfGetClass)
    fun createIrOperation_getEnumValue(oneOfGetEnumValue : Any): Any = arrayOf<Any?>(oneOfGetEnumValue)
    fun createIrOperation_getField(oneOfGetField : Any): Any = arrayOf<Any?>(oneOfGetField)
    fun createIrOperation_getObject(oneOfGetObject : Any): Any = arrayOf<Any?>(oneOfGetObject)
    fun createIrOperation_getValue(oneOfGetValue : Any): Any = arrayOf<Any?>(oneOfGetValue)
    fun createIrOperation_instanceInitializerCall(oneOfInstanceInitializerCall : Any): Any = arrayOf<Any?>(oneOfInstanceInitializerCall)
    fun createIrOperation_propertyReference(oneOfPropertyReference : Any): Any = arrayOf<Any?>(oneOfPropertyReference)
    fun createIrOperation_return_(oneOfReturn : Any): Any = arrayOf<Any?>(oneOfReturn)
    fun createIrOperation_setField(oneOfSetField : Any): Any = arrayOf<Any?>(oneOfSetField)
    fun createIrOperation_setVariable(oneOfSetVariable : Any): Any = arrayOf<Any?>(oneOfSetVariable)
    fun createIrOperation_stringConcat(oneOfStringConcat : Any): Any = arrayOf<Any?>(oneOfStringConcat)
    fun createIrOperation_throw_(oneOfThrow : Any): Any = arrayOf<Any?>(oneOfThrow)
    fun createIrOperation_try_(oneOfTry : Any): Any = arrayOf<Any?>(oneOfTry)
    fun createIrOperation_typeOp(oneOfTypeOp : Any): Any = arrayOf<Any?>(oneOfTypeOp)
    fun createIrOperation_vararg(oneOfVararg : Any): Any = arrayOf<Any?>(oneOfVararg)
    fun createIrOperation_when_(oneOfWhen : Any): Any = arrayOf<Any?>(oneOfWhen)
    fun createIrOperation_while_(oneOfWhile : Any): Any = arrayOf<Any?>(oneOfWhile)
    fun createIrOperation_dynamicMember(oneOfDynamicMember : Any): Any = arrayOf<Any?>(oneOfDynamicMember)
    fun createIrOperation_dynamicOperator(oneOfDynamicOperator : Any): Any = arrayOf<Any?>(oneOfDynamicOperator)
    fun createIrOperation_localDelegatedPropertyReference(oneOfLocalDelegatedPropertyReference : Any): Any = arrayOf<Any?>(oneOfLocalDelegatedPropertyReference)
    fun createIrOperation_constructorCall(oneOfConstructorCall : Any): Any = arrayOf<Any?>(oneOfConstructorCall)
    fun createIrOperation_functionExpression(oneOfFunctionExpression : Any): Any = arrayOf<Any?>(oneOfFunctionExpression)

    fun createIrTypeOperator(index: Int): Any = index

    fun createIrExpression(operation : Any, type_ : Any, coordinatesStartOffset : Int, coordinatesEndOffset : Int): Any = arrayOf<Any?>(operation, type_, coordinatesStartOffset, coordinatesEndOffset)

    fun createNullableIrExpression(expression : Any?): Any = arrayOf<Any?>(expression)

    fun createIrFunction(baseBaseSymbol : Any, baseBaseOrigin : Any, baseBaseCoordinatesStartOffset : Int, baseBaseCoordinatesEndOffset : Int, baseBaseAnnotations : Any, baseName : Any, baseVisibility : Any, baseIsInline : Boolean, baseIsExternal : Boolean, baseReturnType : Any, modality : Any, isTailrec : Boolean, isSuspend : Boolean, overridden : List<Any>): Any = arrayOf<Any?>(baseBaseSymbol, baseBaseOrigin, baseBaseCoordinatesStartOffset, baseBaseCoordinatesEndOffset, baseBaseAnnotations, baseName, baseVisibility, baseIsInline, baseIsExternal, baseReturnType, modality, isTailrec, isSuspend, overridden)
    fun createIrFunction1(partial: Any, baseTypeParameters : Any, baseDispatchReceiver : Any?, baseExtensionReceiver : Any?, baseValueParameter : List<Any>, baseBody : Any?): Any = arrayOf<Any?>(baseTypeParameters, baseDispatchReceiver, baseExtensionReceiver, baseValueParameter, baseBody)

    fun createIrConstructor(baseBaseSymbol : Any, baseBaseOrigin : Any, baseBaseCoordinatesStartOffset : Int, baseBaseCoordinatesEndOffset : Int, baseBaseAnnotations : Any, baseName : Any, baseVisibility : Any, baseIsInline : Boolean, baseIsExternal : Boolean, baseReturnType : Any, isPrimary : Boolean): Any = arrayOf<Any?>(baseBaseSymbol, baseBaseOrigin, baseBaseCoordinatesStartOffset, baseBaseCoordinatesEndOffset, baseBaseAnnotations, baseName, baseVisibility, baseIsInline, baseIsExternal, baseReturnType, isPrimary)
    fun createIrConstructor1(partial: Any, baseTypeParameters : Any, baseDispatchReceiver : Any?, baseExtensionReceiver : Any?, baseValueParameter : List<Any>, baseBody : Any?): Any = arrayOf<Any?>(baseTypeParameters, baseDispatchReceiver, baseExtensionReceiver, baseValueParameter, baseBody)

    fun createIrField(baseSymbol : Any, baseOrigin : Any, baseCoordinatesStartOffset : Int, baseCoordinatesEndOffset : Int, baseAnnotations : Any, initializer : Any?, name : Any, visibility : Any, isFinal : Boolean, isExternal : Boolean, isStatic : Boolean, type_ : Any): Any = arrayOf<Any?>(baseSymbol, baseOrigin, baseCoordinatesStartOffset, baseCoordinatesEndOffset, baseAnnotations, initializer, name, visibility, isFinal, isExternal, isStatic, type_)

    fun createIrLocalDelegatedProperty(baseSymbol : Any, baseOrigin : Any, baseCoordinatesStartOffset : Int, baseCoordinatesEndOffset : Int, baseAnnotations : Any, name : Any, type_ : Any, isVar : Boolean, delegate : Any, getter : Any?, setter : Any?): Any = arrayOf<Any?>(baseSymbol, baseOrigin, baseCoordinatesStartOffset, baseCoordinatesEndOffset, baseAnnotations, name, type_, isVar, delegate, getter, setter)

    fun createIrProperty(baseSymbol : Any, baseOrigin : Any, baseCoordinatesStartOffset : Int, baseCoordinatesEndOffset : Int, baseAnnotations : Any, name : Any, visibility : Any, modality : Any, isVar : Boolean, isConst : Boolean, isLateinit : Boolean, isDelegated : Boolean, isExternal : Boolean, backingField : Any?, getter : Any?, setter : Any?): Any = arrayOf<Any?>(baseSymbol, baseOrigin, baseCoordinatesStartOffset, baseCoordinatesEndOffset, baseAnnotations, name, visibility, modality, isVar, isConst, isLateinit, isDelegated, isExternal, backingField, getter, setter)

    fun createIrVariable(baseSymbol : Any, baseOrigin : Any, baseCoordinatesStartOffset : Int, baseCoordinatesEndOffset : Int, baseAnnotations : Any, name : Any, type_ : Any, isVar : Boolean, isConst : Boolean, isLateinit : Boolean, initializer : Any?): Any = arrayOf<Any?>(baseSymbol, baseOrigin, baseCoordinatesStartOffset, baseCoordinatesEndOffset, baseAnnotations, name, type_, isVar, isConst, isLateinit, initializer)

    fun createClassKind(index: Int): Any = index

    fun createModalityKind(index: Int): Any = index

    fun createIrValueParameter(baseSymbol : Any, baseOrigin : Any, baseCoordinatesStartOffset : Int, baseCoordinatesEndOffset : Int, baseAnnotations : Any, name : Any, index : Int, type_ : Any, varargElementType : Any?, isCrossinline : Boolean, isNoinline : Boolean, defaultValue : Any?): Any = arrayOf<Any?>(baseSymbol, baseOrigin, baseCoordinatesStartOffset, baseCoordinatesEndOffset, baseAnnotations, name, index, type_, varargElementType, isCrossinline, isNoinline, defaultValue)

    fun createIrTypeParameter(baseSymbol : Any, baseOrigin : Any, baseCoordinatesStartOffset : Int, baseCoordinatesEndOffset : Int, baseAnnotations : Any, name : Any, index : Int, variance : Any, superType : List<Any>, isReified : Boolean): Any = arrayOf<Any?>(baseSymbol, baseOrigin, baseCoordinatesStartOffset, baseCoordinatesEndOffset, baseAnnotations, name, index, variance, superType, isReified)

    fun createIrTypeParameterContainer(typeParameter : List<Any>): Any = arrayOf<Any?>(typeParameter)

    fun createIrClass(baseSymbol : Any, baseOrigin : Any, baseCoordinatesStartOffset : Int, baseCoordinatesEndOffset : Int, baseAnnotations : Any, name : Any, kind : Any, visibility : Any, modality : Any, isCompanion : Boolean, isInner : Boolean, isData : Boolean, isExternal : Boolean, isInline : Boolean, superType : List<Any>): Any = arrayOf<Any?>(baseSymbol, baseOrigin, baseCoordinatesStartOffset, baseCoordinatesEndOffset, baseAnnotations, name, kind, visibility, modality, isCompanion, isInner, isData, isExternal, isInline, superType)
    fun createIrClass1(partial: Any, thisReceiver : Any?, typeParameters : Any): Any = arrayOf<Any?>(thisReceiver, typeParameters)
    fun createIrClass2(partial: Any, declarationContainer : Any): Any = arrayOf<Any?>(declarationContainer)

    fun createIrTypeAlias(baseSymbol : Any, baseOrigin : Any, baseCoordinatesStartOffset : Int, baseCoordinatesEndOffset : Int, baseAnnotations : Any, name : Any, visibility : Any, typeParameters : Any, expandedType : Any, isActual : Boolean): Any = arrayOf<Any?>(baseSymbol, baseOrigin, baseCoordinatesStartOffset, baseCoordinatesEndOffset, baseAnnotations, name, visibility, typeParameters, expandedType, isActual)

    fun createIrEnumEntry(baseSymbol : Any, baseOrigin : Any, baseCoordinatesStartOffset : Int, baseCoordinatesEndOffset : Int, baseAnnotations : Any, initializer : Any?, correspondingClass : Any?, name : Any): Any = arrayOf<Any?>(baseSymbol, baseOrigin, baseCoordinatesStartOffset, baseCoordinatesEndOffset, baseAnnotations, initializer, correspondingClass, name)

    fun createIrAnonymousInit(baseSymbol : Any, baseOrigin : Any, baseCoordinatesStartOffset : Int, baseCoordinatesEndOffset : Int, baseAnnotations : Any, body : Any): Any = arrayOf<Any?>(baseSymbol, baseOrigin, baseCoordinatesStartOffset, baseCoordinatesEndOffset, baseAnnotations, body)

    fun createIrDeclaration_irAnonymousInit(oneOfIrAnonymousInit : Any): Any = arrayOf<Any?>(oneOfIrAnonymousInit)
    fun createIrDeclaration_irClass(oneOfIrClass : Any): Any = arrayOf<Any?>(oneOfIrClass)
    fun createIrDeclaration_irConstructor(oneOfIrConstructor : Any): Any = arrayOf<Any?>(oneOfIrConstructor)
    fun createIrDeclaration_irEnumEntry(oneOfIrEnumEntry : Any): Any = arrayOf<Any?>(oneOfIrEnumEntry)
    fun createIrDeclaration_irField(oneOfIrField : Any): Any = arrayOf<Any?>(oneOfIrField)
    fun createIrDeclaration_irFunction(oneOfIrFunction : Any): Any = arrayOf<Any?>(oneOfIrFunction)
    fun createIrDeclaration_irProperty(oneOfIrProperty : Any): Any = arrayOf<Any?>(oneOfIrProperty)
    fun createIrDeclaration_irTypeParameter(oneOfIrTypeParameter : Any): Any = arrayOf<Any?>(oneOfIrTypeParameter)
    fun createIrDeclaration_irVariable(oneOfIrVariable : Any): Any = arrayOf<Any?>(oneOfIrVariable)
    fun createIrDeclaration_irValueParameter(oneOfIrValueParameter : Any): Any = arrayOf<Any?>(oneOfIrValueParameter)
    fun createIrDeclaration_irLocalDelegatedProperty(oneOfIrLocalDelegatedProperty : Any): Any = arrayOf<Any?>(oneOfIrLocalDelegatedProperty)
    fun createIrDeclaration_irTypeAlias(oneOfIrTypeAlias : Any): Any = arrayOf<Any?>(oneOfIrTypeAlias)

    fun createIrBranch(condition : Any, result : Any): Any = arrayOf<Any?>(condition, result)

    fun createIrBlockBody(statement : List<Any>): Any = arrayOf<Any?>(statement)

    fun createIrCatch(catchParameter : Any, result : Any): Any = arrayOf<Any?>(catchParameter, result)

    fun createIrSyntheticBodyKind(index: Int): Any = index

    fun createIrSyntheticBody(kind : Any): Any = arrayOf<Any?>(kind)

    fun createIrStatement_declaration(coordinatesStartOffset : Int, coordinatesEndOffset : Int, oneOfDeclaration : Any): Any = arrayOf<Any?>(coordinatesStartOffset, coordinatesEndOffset, oneOfDeclaration)
    fun createIrStatement_expression(coordinatesStartOffset : Int, coordinatesEndOffset : Int, oneOfExpression : Any): Any = arrayOf<Any?>(coordinatesStartOffset, coordinatesEndOffset, oneOfExpression)
    fun createIrStatement_blockBody(coordinatesStartOffset : Int, coordinatesEndOffset : Int, oneOfBlockBody : Any): Any = arrayOf<Any?>(coordinatesStartOffset, coordinatesEndOffset, oneOfBlockBody)
    fun createIrStatement_branch(coordinatesStartOffset : Int, coordinatesEndOffset : Int, oneOfBranch : Any): Any = arrayOf<Any?>(coordinatesStartOffset, coordinatesEndOffset, oneOfBranch)
    fun createIrStatement_catch(coordinatesStartOffset : Int, coordinatesEndOffset : Int, oneOfCatch : Any): Any = arrayOf<Any?>(coordinatesStartOffset, coordinatesEndOffset, oneOfCatch)
    fun createIrStatement_syntheticBody(coordinatesStartOffset : Int, coordinatesEndOffset : Int, oneOfSyntheticBody : Any): Any = arrayOf<Any?>(coordinatesStartOffset, coordinatesEndOffset, oneOfSyntheticBody)

    open fun readDescriptorReference(): Any {
        var packageFqName: Any? = null
        var classFqName: Any? = null
        var name: Any? = null
        var uniqId: Any? = null
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

    open fun readUniqId(): Any {
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

    open fun readVisibility(): Any {
        var name: Any? = null
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

    open fun readIrStatementOrigin(): Any {
        var name: Any? = null
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

    open fun readIrDeclarationOrigin(): Any {
        var oneOfOrigin: Any? = null
        var oneOfCustom: Any? = null
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

    open fun readIrDataIndex(): Any {
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

    open fun readFqName(): Any {
        var segment: MutableList<Any> = mutableListOf()
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

    open fun readIrDeclarationContainer(): Any {
        var declaration: MutableList<Any> = mutableListOf()
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

    open fun readFileEntry(): Any {
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

    open fun readIrFile(): Any {
        var declarationId: MutableList<Any> = mutableListOf()
        var fileEntry: Any? = null
        var fqName: Any? = null
        var annotations: Any? = null
        var explicitlyExportedToCompiler: MutableList<Any> = mutableListOf()
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

    open fun readStringTable(): Any {
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

    open fun readIrSymbolData(): Any {
        var kind: Any? = null
        var uniqId: Any? = null
        var topLevelUniqId: Any? = null
        var fqname: Any? = null
        var descriptorReference: Any? = null
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

    open fun readIrSymbolTable(): Any {
        var symbols: MutableList<Any> = mutableListOf()
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

    open fun readAnnotations(): Any {
        var annotation: MutableList<Any> = mutableListOf()
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

    open fun readTypeArguments(): Any {
        var typeArgument: MutableList<Any> = mutableListOf()
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

    open fun readIrStarProjection(): Any {
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

    open fun readIrTypeProjection(): Any {
        var variance: Any? = null
        var type_: Any? = null
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

    open fun readIrTypeArgument(): Any {
        var oneOfStar: Any? = null
        var oneOfType: Any? = null
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

    open fun readIrSimpleType(): Any {
        var annotations: Any? = null
        var classifier: Any? = null
        var hasQuestionMark: Boolean = false
        var argument: MutableList<Any> = mutableListOf()
        var abbreviation: Any? = null
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

    open fun readIrTypeAbbreviation(): Any {
        var annotations: Any? = null
        var typeAlias: Any? = null
        var hasQuestionMark: Boolean = false
        var argument: MutableList<Any> = mutableListOf()
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

    open fun readIrDynamicType(): Any {
        var annotations: Any? = null
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

    open fun readIrErrorType(): Any {
        var annotations: Any? = null
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

    open fun readIrType(): Any {
        var oneOfSimple: Any? = null
        var oneOfDynamic: Any? = null
        var oneOfError: Any? = null
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

    open fun readIrTypeTable(): Any {
        var types: MutableList<Any> = mutableListOf()
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

    open fun readIrBreak(): Any {
        var loopId: Int = 0
        var label: Any? = null
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

    open fun readIrBlock(): Any {
        var origin: Any? = null
        var statement: MutableList<Any> = mutableListOf()
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

    open fun readIrCall(): Any {
        var symbol: Any? = null
        var memberAccessDispatchReceiver: Any? = null
        var memberAccessExtensionReceiver: Any? = null
        var memberAccessValueArgument: MutableList<Any> = mutableListOf()
        var memberAccessTypeArguments: Any? = null
        var super_: Any? = null
        var origin: Any? = null
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

    open fun readIrConstructorCall(): Any {
        var symbol: Any? = null
        var constructorTypeArgumentsCount: Int = 0
        var memberAccessDispatchReceiver: Any? = null
        var memberAccessExtensionReceiver: Any? = null
        var memberAccessValueArgument: MutableList<Any> = mutableListOf()
        var memberAccessTypeArguments: Any? = null
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

    open fun readIrFunctionReference(): Any {
        var symbol: Any? = null
        var origin: Any? = null
        var memberAccessDispatchReceiver: Any? = null
        var memberAccessExtensionReceiver: Any? = null
        var memberAccessValueArgument: MutableList<Any> = mutableListOf()
        var memberAccessTypeArguments: Any? = null
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

    open fun readIrLocalDelegatedPropertyReference(): Any {
        var delegate: Any? = null
        var getter: Any? = null
        var setter: Any? = null
        var symbol: Any? = null
        var origin: Any? = null
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

    open fun readIrPropertyReference(): Any {
        var field: Any? = null
        var getter: Any? = null
        var setter: Any? = null
        var origin: Any? = null
        var memberAccessDispatchReceiver: Any? = null
        var memberAccessExtensionReceiver: Any? = null
        var memberAccessValueArgument: MutableList<Any> = mutableListOf()
        var memberAccessTypeArguments: Any? = null
        var symbol: Any? = null
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

    open fun readIrComposite(): Any {
        var statement: MutableList<Any> = mutableListOf()
        var origin: Any? = null
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

    open fun readIrClassReference(): Any {
        var classSymbol: Any? = null
        var classType: Any? = null
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

    open fun readIrConst(): Any {
        var oneOfNull: Boolean? = null
        var oneOfBoolean: Boolean? = null
        var oneOfChar: Int? = null
        var oneOfByte: Int? = null
        var oneOfShort: Int? = null
        var oneOfInt: Int? = null
        var oneOfLong: Long? = null
        var oneOfFloat: Float? = null
        var oneOfDouble: Double? = null
        var oneOfString: Any? = null
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

    open fun readIrContinue(): Any {
        var loopId: Int = 0
        var label: Any? = null
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

    open fun readIrDelegatingConstructorCall(): Any {
        var symbol: Any? = null
        var memberAccessDispatchReceiver: Any? = null
        var memberAccessExtensionReceiver: Any? = null
        var memberAccessValueArgument: MutableList<Any> = mutableListOf()
        var memberAccessTypeArguments: Any? = null
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

    open fun readIrDoWhile(): Any {
        var loopLoopId: Int = 0
        var loopCondition: Any? = null
        var loopLabel: Any? = null
        var loopBody: Any? = null
        var loopBodyOffset: Int = -1
        var loopOrigin: Any? = null
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

    open fun readIrEnumConstructorCall(): Any {
        var symbol: Any? = null
        var memberAccessDispatchReceiver: Any? = null
        var memberAccessExtensionReceiver: Any? = null
        var memberAccessValueArgument: MutableList<Any> = mutableListOf()
        var memberAccessTypeArguments: Any? = null
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

    open fun readIrGetClass(): Any {
        var argument: Any? = null
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

    open fun readIrGetEnumValue(): Any {
        var symbol: Any? = null
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

    open fun readIrGetField(): Any {
        var fieldAccessSymbol: Any? = null
        var fieldAccessSuper: Any? = null
        var fieldAccessReceiver: Any? = null
        var origin: Any? = null
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

    open fun readIrGetValue(): Any {
        var symbol: Any? = null
        var origin: Any? = null
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

    open fun readIrGetObject(): Any {
        var symbol: Any? = null
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

    open fun readIrInstanceInitializerCall(): Any {
        var symbol: Any? = null
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

    open fun readIrReturn(): Any {
        var returnTarget: Any? = null
        var value: Any? = null
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

    open fun readIrSetField(): Any {
        var fieldAccessSymbol: Any? = null
        var fieldAccessSuper: Any? = null
        var fieldAccessReceiver: Any? = null
        var value: Any? = null
        var origin: Any? = null
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

    open fun readIrSetVariable(): Any {
        var symbol: Any? = null
        var value: Any? = null
        var origin: Any? = null
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

    open fun readIrSpreadElement(): Any {
        var expression: Any? = null
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

    open fun readIrStringConcat(): Any {
        var argument: MutableList<Any> = mutableListOf()
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

    open fun readIrThrow(): Any {
        var value: Any? = null
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

    open fun readIrTry(): Any {
        var result: Any? = null
        var catch: MutableList<Any> = mutableListOf()
        var finally: Any? = null
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

    open fun readIrTypeOp(): Any {
        var operator: Any? = null
        var operand: Any? = null
        var argument: Any? = null
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

    open fun readIrVararg(): Any {
        var elementType: Any? = null
        var element: MutableList<Any> = mutableListOf()
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

    open fun readIrVarargElement(): Any {
        var oneOfExpression: Any? = null
        var oneOfSpreadElement: Any? = null
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

    open fun readIrWhen(): Any {
        var branch: MutableList<Any> = mutableListOf()
        var origin: Any? = null
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

    open fun readIrWhile(): Any {
        var loopLoopId: Int = 0
        var loopCondition: Any? = null
        var loopLabel: Any? = null
        var loopBody: Any? = null
        var loopBodyOffset: Int = -1
        var loopOrigin: Any? = null
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

    open fun readIrFunctionExpression(): Any {
        var function: Any? = null
        var origin: Any? = null
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

    open fun readIrDynamicMemberExpression(): Any {
        var memberName: Any? = null
        var receiver: Any? = null
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

    open fun readIrDynamicOperatorExpression(): Any {
        var operator: Any? = null
        var receiver: Any? = null
        var argument: MutableList<Any> = mutableListOf()
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

    open fun readIrOperation(): Any {
        var oneOfBlock: Any? = null
        var oneOfBreak: Any? = null
        var oneOfCall: Any? = null
        var oneOfClassReference: Any? = null
        var oneOfComposite: Any? = null
        var oneOfConst: Any? = null
        var oneOfContinue: Any? = null
        var oneOfDelegatingConstructorCall: Any? = null
        var oneOfDoWhile: Any? = null
        var oneOfEnumConstructorCall: Any? = null
        var oneOfFunctionReference: Any? = null
        var oneOfGetClass: Any? = null
        var oneOfGetEnumValue: Any? = null
        var oneOfGetField: Any? = null
        var oneOfGetObject: Any? = null
        var oneOfGetValue: Any? = null
        var oneOfInstanceInitializerCall: Any? = null
        var oneOfPropertyReference: Any? = null
        var oneOfReturn: Any? = null
        var oneOfSetField: Any? = null
        var oneOfSetVariable: Any? = null
        var oneOfStringConcat: Any? = null
        var oneOfThrow: Any? = null
        var oneOfTry: Any? = null
        var oneOfTypeOp: Any? = null
        var oneOfVararg: Any? = null
        var oneOfWhen: Any? = null
        var oneOfWhile: Any? = null
        var oneOfDynamicMember: Any? = null
        var oneOfDynamicOperator: Any? = null
        var oneOfLocalDelegatedPropertyReference: Any? = null
        var oneOfConstructorCall: Any? = null
        var oneOfFunctionExpression: Any? = null
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

    protected var fieldIrExpressionType: Any? = null
    protected var fieldIrExpressionCoordinatesStartOffset: Int = 0
    protected var fieldIrExpressionCoordinatesEndOffset: Int = 0

    open fun readIrExpression(): Any {
        var operation: Any? = null
        var operationOffset: Int = -1
        var type_: Any? = null
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

    open fun readNullableIrExpression(): Any {
        var expression: Any? = null
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

    open fun readIrFunction(): Any {
        var baseBaseSymbol: Any? = null
        var baseBaseOrigin: Any? = null
        var baseBaseCoordinatesStartOffset: Int = 0
        var baseBaseCoordinatesEndOffset: Int = 0
        var baseBaseAnnotations: Any? = null
        var baseName: Any? = null
        var baseVisibility: Any? = null
        var baseIsInline: Boolean = false
        var baseIsExternal: Boolean = false
        var baseTypeParameters: Any? = null
        var baseTypeParametersOffset: Int = -1
        var baseDispatchReceiver: Any? = null
        var baseDispatchReceiverOffset: Int = -1
        var baseExtensionReceiver: Any? = null
        var baseExtensionReceiverOffset: Int = -1
        var baseValueParameter: MutableList<Any> = mutableListOf()
        var baseValueParameterOffsetList: MutableList<Int> = arrayListOf()
        var baseBody: Any? = null
        var baseBodyOffset: Int = -1
        var baseReturnType: Any? = null
        var modality: Any? = null
        var isTailrec: Boolean = false
        var isSuspend: Boolean = false
        var overridden: MutableList<Any> = mutableListOf()
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

    open fun readIrConstructor(): Any {
        var baseBaseSymbol: Any? = null
        var baseBaseOrigin: Any? = null
        var baseBaseCoordinatesStartOffset: Int = 0
        var baseBaseCoordinatesEndOffset: Int = 0
        var baseBaseAnnotations: Any? = null
        var baseName: Any? = null
        var baseVisibility: Any? = null
        var baseIsInline: Boolean = false
        var baseIsExternal: Boolean = false
        var baseTypeParameters: Any? = null
        var baseTypeParametersOffset: Int = -1
        var baseDispatchReceiver: Any? = null
        var baseDispatchReceiverOffset: Int = -1
        var baseExtensionReceiver: Any? = null
        var baseExtensionReceiverOffset: Int = -1
        var baseValueParameter: MutableList<Any> = mutableListOf()
        var baseValueParameterOffsetList: MutableList<Int> = arrayListOf()
        var baseBody: Any? = null
        var baseBodyOffset: Int = -1
        var baseReturnType: Any? = null
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

    open fun readIrField(): Any {
        var baseSymbol: Any? = null
        var baseOrigin: Any? = null
        var baseCoordinatesStartOffset: Int = 0
        var baseCoordinatesEndOffset: Int = 0
        var baseAnnotations: Any? = null
        var initializer: Any? = null
        var name: Any? = null
        var visibility: Any? = null
        var isFinal: Boolean = false
        var isExternal: Boolean = false
        var isStatic: Boolean = false
        var type_: Any? = null
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

    open fun readIrLocalDelegatedProperty(): Any {
        var baseSymbol: Any? = null
        var baseOrigin: Any? = null
        var baseCoordinatesStartOffset: Int = 0
        var baseCoordinatesEndOffset: Int = 0
        var baseAnnotations: Any? = null
        var name: Any? = null
        var type_: Any? = null
        var isVar: Boolean = false
        var delegate: Any? = null
        var getter: Any? = null
        var setter: Any? = null
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

    open fun readIrProperty(): Any {
        var baseSymbol: Any? = null
        var baseOrigin: Any? = null
        var baseCoordinatesStartOffset: Int = 0
        var baseCoordinatesEndOffset: Int = 0
        var baseAnnotations: Any? = null
        var name: Any? = null
        var visibility: Any? = null
        var modality: Any? = null
        var isVar: Boolean = false
        var isConst: Boolean = false
        var isLateinit: Boolean = false
        var isDelegated: Boolean = false
        var isExternal: Boolean = false
        var backingField: Any? = null
        var getter: Any? = null
        var setter: Any? = null
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

    open fun readIrVariable(): Any {
        var baseSymbol: Any? = null
        var baseOrigin: Any? = null
        var baseCoordinatesStartOffset: Int = 0
        var baseCoordinatesEndOffset: Int = 0
        var baseAnnotations: Any? = null
        var name: Any? = null
        var type_: Any? = null
        var isVar: Boolean = false
        var isConst: Boolean = false
        var isLateinit: Boolean = false
        var initializer: Any? = null
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

    open fun readIrValueParameter(): Any {
        var baseSymbol: Any? = null
        var baseOrigin: Any? = null
        var baseCoordinatesStartOffset: Int = 0
        var baseCoordinatesEndOffset: Int = 0
        var baseAnnotations: Any? = null
        var name: Any? = null
        var index: Int = 0
        var type_: Any? = null
        var varargElementType: Any? = null
        var isCrossinline: Boolean = false
        var isNoinline: Boolean = false
        var defaultValue: Any? = null
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

    open fun readIrTypeParameter(): Any {
        var baseSymbol: Any? = null
        var baseOrigin: Any? = null
        var baseCoordinatesStartOffset: Int = 0
        var baseCoordinatesEndOffset: Int = 0
        var baseAnnotations: Any? = null
        var name: Any? = null
        var index: Int = 0
        var variance: Any? = null
        var superType: MutableList<Any> = mutableListOf()
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

    open fun readIrTypeParameterContainer(): Any {
        var typeParameter: MutableList<Any> = mutableListOf()
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

    open fun readIrClass(): Any {
        var baseSymbol: Any? = null
        var baseOrigin: Any? = null
        var baseCoordinatesStartOffset: Int = 0
        var baseCoordinatesEndOffset: Int = 0
        var baseAnnotations: Any? = null
        var name: Any? = null
        var kind: Any? = null
        var visibility: Any? = null
        var modality: Any? = null
        var isCompanion: Boolean = false
        var isInner: Boolean = false
        var isData: Boolean = false
        var isExternal: Boolean = false
        var isInline: Boolean = false
        var thisReceiver: Any? = null
        var thisReceiverOffset: Int = -1
        var typeParameters: Any? = null
        var typeParametersOffset: Int = -1
        var declarationContainer: Any? = null
        var declarationContainerOffset: Int = -1
        var superType: MutableList<Any> = mutableListOf()
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

    open fun readIrTypeAlias(): Any {
        var baseSymbol: Any? = null
        var baseOrigin: Any? = null
        var baseCoordinatesStartOffset: Int = 0
        var baseCoordinatesEndOffset: Int = 0
        var baseAnnotations: Any? = null
        var name: Any? = null
        var visibility: Any? = null
        var typeParameters: Any? = null
        var expandedType: Any? = null
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

    open fun readIrEnumEntry(): Any {
        var baseSymbol: Any? = null
        var baseOrigin: Any? = null
        var baseCoordinatesStartOffset: Int = 0
        var baseCoordinatesEndOffset: Int = 0
        var baseAnnotations: Any? = null
        var initializer: Any? = null
        var correspondingClass: Any? = null
        var name: Any? = null
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

    open fun readIrAnonymousInit(): Any {
        var baseSymbol: Any? = null
        var baseOrigin: Any? = null
        var baseCoordinatesStartOffset: Int = 0
        var baseCoordinatesEndOffset: Int = 0
        var baseAnnotations: Any? = null
        var body: Any? = null
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

    open fun readIrDeclaration(): Any {
        var oneOfIrAnonymousInit: Any? = null
        var oneOfIrClass: Any? = null
        var oneOfIrConstructor: Any? = null
        var oneOfIrEnumEntry: Any? = null
        var oneOfIrField: Any? = null
        var oneOfIrFunction: Any? = null
        var oneOfIrProperty: Any? = null
        var oneOfIrTypeParameter: Any? = null
        var oneOfIrVariable: Any? = null
        var oneOfIrValueParameter: Any? = null
        var oneOfIrLocalDelegatedProperty: Any? = null
        var oneOfIrTypeAlias: Any? = null
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

    open fun readIrBranch(): Any {
        var condition: Any? = null
        var result: Any? = null
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

    open fun readIrBlockBody(): Any {
        var statement: MutableList<Any> = mutableListOf()
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

    open fun readIrCatch(): Any {
        var catchParameter: Any? = null
        var result: Any? = null
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

    open fun readIrSyntheticBody(): Any {
        var kind: Any? = null
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

    open fun readIrStatement(): Any {
        var coordinatesStartOffset: Int = 0
        var coordinatesEndOffset: Int = 0
        var oneOfDeclaration: Any? = null
        var oneOfDeclarationOffset: Int = -1
        var oneOfExpression: Any? = null
        var oneOfExpressionOffset: Int = -1
        var oneOfBlockBody: Any? = null
        var oneOfBlockBodyOffset: Int = -1
        var oneOfBranch: Any? = null
        var oneOfBranchOffset: Int = -1
        var oneOfCatch: Any? = null
        var oneOfCatchOffset: Int = -1
        var oneOfSyntheticBody: Any? = null
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