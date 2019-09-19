/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.nextgen

class SimpleIrProtoReader(source: ByteArray) : ProtoReader(source) {
    fun createDescriptorReference(packageFqName : Any, classFqName : Any, name : Any, uniqId : Any?, isGetter : Boolean?, isSetter : Boolean?, isBackingField : Boolean?, isFakeOverride : Boolean?, isDefaultConstructor : Boolean?, isEnumEntry : Boolean?, isEnumSpecial : Boolean?, isTypeParameter : Boolean?): Any = arrayOf<Any?>(packageFqName, classFqName, name, uniqId, isGetter, isSetter, isBackingField, isFakeOverride, isDefaultConstructor, isEnumEntry, isEnumSpecial, isTypeParameter)

    fun createUniqId(index : Long, isLocal : Boolean): Any = arrayOf<Any?>(index, isLocal)

    fun createCoordinates(startOffset : Int, endOffset : Int): Any = arrayOf<Any?>(startOffset, endOffset)

    fun createVisibility(name : Any): Any = arrayOf<Any?>(name)

    fun createIrStatementOrigin(name : Any): Any = arrayOf<Any?>(name)

    fun createKnownOrigin(index: Int): Any = index

    fun createIrDeclarationOrigin_origin(origin : Any): Any = arrayOf<Any?>(origin)
    fun createIrDeclarationOrigin_custom(custom : Any): Any = arrayOf<Any?>(custom)

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

    fun createIrTypeArgument_star(star : Any): Any = arrayOf<Any?>(star)
    fun createIrTypeArgument_type_(type_ : Any): Any = arrayOf<Any?>(type_)

    fun createIrSimpleType(annotations : Any, classifier : Any, hasQuestionMark : Boolean, argument : List<Any>, abbreviation : Any?): Any = arrayOf<Any?>(annotations, classifier, hasQuestionMark, argument, abbreviation)

    fun createIrTypeAbbreviation(annotations : Any, typeAlias : Any, hasQuestionMark : Boolean, argument : List<Any>): Any = arrayOf<Any?>(annotations, typeAlias, hasQuestionMark, argument)

    fun createIrDynamicType(annotations : Any): Any = arrayOf<Any?>(annotations)

    fun createIrErrorType(annotations : Any): Any = arrayOf<Any?>(annotations)

    fun createIrType_simple(simple : Any): Any = arrayOf<Any?>(simple)
    fun createIrType_dynamic(dynamic : Any): Any = arrayOf<Any?>(dynamic)
    fun createIrType_error(error : Any): Any = arrayOf<Any?>(error)

    fun createIrTypeTable(types : List<Any>): Any = arrayOf<Any?>(types)

    fun createIrBreak(loopId : Int, label : Any?): Any = arrayOf<Any?>(loopId, label)

    fun createIrBlock(origin : Any?, statement : List<Any>): Any = arrayOf<Any?>(origin, statement)

    fun createMemberAccessCommon(dispatchReceiver : Any?, extensionReceiver : Any?, valueArgument : List<Any>, typeArguments : Any): Any = arrayOf<Any?>(dispatchReceiver, extensionReceiver, valueArgument, typeArguments)

    fun createIrCall(symbol : Any, memberAccess : Any, super_ : Any?, origin : Any?): Any = arrayOf<Any?>(symbol, memberAccess, super_, origin)

    fun createIrConstructorCall(symbol : Any, constructorTypeArgumentsCount : Int, memberAccess : Any): Any = arrayOf<Any?>(symbol, constructorTypeArgumentsCount, memberAccess)

    fun createIrFunctionReference(symbol : Any, origin : Any?, memberAccess : Any): Any = arrayOf<Any?>(symbol, origin, memberAccess)

    fun createIrLocalDelegatedPropertyReference(delegate : Any, getter : Any?, setter : Any?, symbol : Any, origin : Any?): Any = arrayOf<Any?>(delegate, getter, setter, symbol, origin)

    fun createIrPropertyReference(field : Any?, getter : Any?, setter : Any?, origin : Any?, memberAccess : Any, symbol : Any): Any = arrayOf<Any?>(field, getter, setter, origin, memberAccess, symbol)

    fun createIrComposite(statement : List<Any>, origin : Any?): Any = arrayOf<Any?>(statement, origin)

    fun createIrClassReference(classSymbol : Any, classType : Any): Any = arrayOf<Any?>(classSymbol, classType)

    fun createIrConst_null_(null_ : Boolean): Any = arrayOf<Any?>(null_)
    fun createIrConst_boolean(boolean : Boolean): Any = arrayOf<Any?>(boolean)
    fun createIrConst_char(char : Int): Any = arrayOf<Any?>(char)
    fun createIrConst_byte(byte : Int): Any = arrayOf<Any?>(byte)
    fun createIrConst_short(short : Int): Any = arrayOf<Any?>(short)
    fun createIrConst_int(int : Int): Any = arrayOf<Any?>(int)
    fun createIrConst_long(long : Long): Any = arrayOf<Any?>(long)
    fun createIrConst_float(float : Float): Any = arrayOf<Any?>(float)
    fun createIrConst_double(double : Double): Any = arrayOf<Any?>(double)
    fun createIrConst_string(string : Any): Any = arrayOf<Any?>(string)

    fun createIrContinue(loopId : Int, label : Any?): Any = arrayOf<Any?>(loopId, label)

    fun createIrDelegatingConstructorCall(symbol : Any, memberAccess : Any): Any = arrayOf<Any?>(symbol, memberAccess)

    fun createIrDoWhile(loop : Any): Any = arrayOf<Any?>(loop)

    fun createIrEnumConstructorCall(symbol : Any, memberAccess : Any): Any = arrayOf<Any?>(symbol, memberAccess)

    fun createIrGetClass(argument : Any): Any = arrayOf<Any?>(argument)

    fun createIrGetEnumValue(symbol : Any): Any = arrayOf<Any?>(symbol)

    fun createFieldAccessCommon(symbol : Any, super_ : Any?, receiver : Any?): Any = arrayOf<Any?>(symbol, super_, receiver)

    fun createIrGetField(fieldAccess : Any, origin : Any?): Any = arrayOf<Any?>(fieldAccess, origin)

    fun createIrGetValue(symbol : Any, origin : Any?): Any = arrayOf<Any?>(symbol, origin)

    fun createIrGetObject(symbol : Any): Any = arrayOf<Any?>(symbol)

    fun createIrInstanceInitializerCall(symbol : Any): Any = arrayOf<Any?>(symbol)

    fun createLoop(loopId : Int, condition : Any, label : Any?, body : Any?, origin : Any?): Any = arrayOf<Any?>(loopId, condition, label, body, origin)

    fun createIrReturn(returnTarget : Any, value : Any): Any = arrayOf<Any?>(returnTarget, value)

    fun createIrSetField(fieldAccess : Any, value : Any, origin : Any?): Any = arrayOf<Any?>(fieldAccess, value, origin)

    fun createIrSetVariable(symbol : Any, value : Any, origin : Any?): Any = arrayOf<Any?>(symbol, value, origin)

    fun createIrSpreadElement(expression : Any, coordinates : Any): Any = arrayOf<Any?>(expression, coordinates)

    fun createIrStringConcat(argument : List<Any>): Any = arrayOf<Any?>(argument)

    fun createIrThrow(value : Any): Any = arrayOf<Any?>(value)

    fun createIrTry(result : Any, catch : List<Any>, finally : Any?): Any = arrayOf<Any?>(result, catch, finally)

    fun createIrTypeOp(operator : Any, operand : Any, argument : Any): Any = arrayOf<Any?>(operator, operand, argument)

    fun createIrVararg(elementType : Any, element : List<Any>): Any = arrayOf<Any?>(elementType, element)

    fun createIrVarargElement_expression(expression : Any): Any = arrayOf<Any?>(expression)
    fun createIrVarargElement_spreadElement(spreadElement : Any): Any = arrayOf<Any?>(spreadElement)

    fun createIrWhen(branch : List<Any>, origin : Any?): Any = arrayOf<Any?>(branch, origin)

    fun createIrWhile(loop : Any): Any = arrayOf<Any?>(loop)

    fun createIrFunctionExpression(function : Any, origin : Any): Any = arrayOf<Any?>(function, origin)

    fun createIrDynamicMemberExpression(memberName : Any, receiver : Any): Any = arrayOf<Any?>(memberName, receiver)

    fun createIrDynamicOperator(index: Int): Any = index

    fun createIrDynamicOperatorExpression(operator : Any, receiver : Any, argument : List<Any>): Any = arrayOf<Any?>(operator, receiver, argument)

    fun createIrOperation_block(block : Any): Any = arrayOf<Any?>(block)
    fun createIrOperation_break_(break_ : Any): Any = arrayOf<Any?>(break_)
    fun createIrOperation_call(call : Any): Any = arrayOf<Any?>(call)
    fun createIrOperation_classReference(classReference : Any): Any = arrayOf<Any?>(classReference)
    fun createIrOperation_composite(composite : Any): Any = arrayOf<Any?>(composite)
    fun createIrOperation_const(const : Any): Any = arrayOf<Any?>(const)
    fun createIrOperation_continue_(continue_ : Any): Any = arrayOf<Any?>(continue_)
    fun createIrOperation_delegatingConstructorCall(delegatingConstructorCall : Any): Any = arrayOf<Any?>(delegatingConstructorCall)
    fun createIrOperation_doWhile(doWhile : Any): Any = arrayOf<Any?>(doWhile)
    fun createIrOperation_enumConstructorCall(enumConstructorCall : Any): Any = arrayOf<Any?>(enumConstructorCall)
    fun createIrOperation_functionReference(functionReference : Any): Any = arrayOf<Any?>(functionReference)
    fun createIrOperation_getClass(getClass : Any): Any = arrayOf<Any?>(getClass)
    fun createIrOperation_getEnumValue(getEnumValue : Any): Any = arrayOf<Any?>(getEnumValue)
    fun createIrOperation_getField(getField : Any): Any = arrayOf<Any?>(getField)
    fun createIrOperation_getObject(getObject : Any): Any = arrayOf<Any?>(getObject)
    fun createIrOperation_getValue(getValue : Any): Any = arrayOf<Any?>(getValue)
    fun createIrOperation_instanceInitializerCall(instanceInitializerCall : Any): Any = arrayOf<Any?>(instanceInitializerCall)
    fun createIrOperation_propertyReference(propertyReference : Any): Any = arrayOf<Any?>(propertyReference)
    fun createIrOperation_return_(return_ : Any): Any = arrayOf<Any?>(return_)
    fun createIrOperation_setField(setField : Any): Any = arrayOf<Any?>(setField)
    fun createIrOperation_setVariable(setVariable : Any): Any = arrayOf<Any?>(setVariable)
    fun createIrOperation_stringConcat(stringConcat : Any): Any = arrayOf<Any?>(stringConcat)
    fun createIrOperation_throw_(throw_ : Any): Any = arrayOf<Any?>(throw_)
    fun createIrOperation_try_(try_ : Any): Any = arrayOf<Any?>(try_)
    fun createIrOperation_typeOp(typeOp : Any): Any = arrayOf<Any?>(typeOp)
    fun createIrOperation_vararg(vararg : Any): Any = arrayOf<Any?>(vararg)
    fun createIrOperation_when_(when_ : Any): Any = arrayOf<Any?>(when_)
    fun createIrOperation_while_(while_ : Any): Any = arrayOf<Any?>(while_)
    fun createIrOperation_dynamicMember(dynamicMember : Any): Any = arrayOf<Any?>(dynamicMember)
    fun createIrOperation_dynamicOperator(dynamicOperator : Any): Any = arrayOf<Any?>(dynamicOperator)
    fun createIrOperation_localDelegatedPropertyReference(localDelegatedPropertyReference : Any): Any = arrayOf<Any?>(localDelegatedPropertyReference)
    fun createIrOperation_constructorCall(constructorCall : Any): Any = arrayOf<Any?>(constructorCall)
    fun createIrOperation_functionExpression(functionExpression : Any): Any = arrayOf<Any?>(functionExpression)

    fun createIrTypeOperator(index: Int): Any = index

    fun createIrExpression(operation : Any, type_ : Any, coordinates : Any): Any = arrayOf<Any?>(operation, type_, coordinates)

    fun createNullableIrExpression(expression : Any?): Any = arrayOf<Any?>(expression)

    fun createIrFunction(symbol : Any, origin : Any, coordinates : Any, annotations : Any, name : Any, visibility : Any, isInline : Boolean, isExternal : Boolean, typeParameters : Any, dispatchReceiver : Any?, extensionReceiver : Any?, valueParameter : List<Any>, body : Any?, returnType : Any, modality : Any, isTailrec : Boolean, isSuspend : Boolean, overridden : List<Any>): Any = arrayOf<Any?>(symbol, origin, coordinates, annotations, name, visibility, isInline, isExternal, typeParameters, dispatchReceiver, extensionReceiver, valueParameter, body, returnType, modality, isTailrec, isSuspend, overridden)

    fun createIrConstructor(symbol : Any, origin : Any, coordinates : Any, annotations : Any, name : Any, visibility : Any, isInline : Boolean, isExternal : Boolean, typeParameters : Any, dispatchReceiver : Any?, extensionReceiver : Any?, valueParameter : List<Any>, body : Any?, returnType : Any, isPrimary : Boolean): Any = arrayOf<Any?>(symbol, origin, coordinates, annotations, name, visibility, isInline, isExternal, typeParameters, dispatchReceiver, extensionReceiver, valueParameter, body, returnType, isPrimary)

    fun createIrField(symbol : Any, origin : Any, coordinates : Any, annotations : Any, initializer : Any?, name : Any, visibility : Any, isFinal : Boolean, isExternal : Boolean, isStatic : Boolean, type_ : Any): Any = arrayOf<Any?>(symbol, origin, coordinates, annotations, initializer, name, visibility, isFinal, isExternal, isStatic, type_)

    fun createIrLocalDelegatedProperty(symbol : Any, origin : Any, coordinates : Any, annotations : Any, name : Any, type_ : Any, isVar : Boolean, delegate : Any, getter : Any?, setter : Any?): Any = arrayOf<Any?>(symbol, origin, coordinates, annotations, name, type_, isVar, delegate, getter, setter)

    fun createIrProperty(symbol : Any, origin : Any, coordinates : Any, annotations : Any, name : Any, visibility : Any, modality : Any, isVar : Boolean, isConst : Boolean, isLateinit : Boolean, isDelegated : Boolean, isExternal : Boolean, backingField : Any?, getter : Any?, setter : Any?): Any = arrayOf<Any?>(symbol, origin, coordinates, annotations, name, visibility, modality, isVar, isConst, isLateinit, isDelegated, isExternal, backingField, getter, setter)

    fun createIrVariable(symbol : Any, origin : Any, coordinates : Any, annotations : Any, name : Any, type_ : Any, isVar : Boolean, isConst : Boolean, isLateinit : Boolean, initializer : Any?): Any = arrayOf<Any?>(symbol, origin, coordinates, annotations, name, type_, isVar, isConst, isLateinit, initializer)

    fun createClassKind(index: Int): Any = index

    fun createModalityKind(index: Int): Any = index

    fun createIrValueParameter(symbol : Any, origin : Any, coordinates : Any, annotations : Any, name : Any, index : Int, type_ : Any, varargElementType : Any?, isCrossinline : Boolean, isNoinline : Boolean, defaultValue : Any?): Any = arrayOf<Any?>(symbol, origin, coordinates, annotations, name, index, type_, varargElementType, isCrossinline, isNoinline, defaultValue)

    fun createIrTypeParameter(symbol : Any, origin : Any, coordinates : Any, annotations : Any, name : Any, index : Int, variance : Any, superType : List<Any>, isReified : Boolean): Any = arrayOf<Any?>(symbol, origin, coordinates, annotations, name, index, variance, superType, isReified)

    fun createIrTypeParameterContainer(typeParameter : List<Any>): Any = arrayOf<Any?>(typeParameter)

    fun createIrClass(symbol : Any, origin : Any, coordinates : Any, annotations : Any, name : Any, kind : Any, visibility : Any, modality : Any, isCompanion : Boolean, isInner : Boolean, isData : Boolean, isExternal : Boolean, isInline : Boolean, thisReceiver : Any?, typeParameters : Any, declarationContainer : Any, superType : List<Any>): Any = arrayOf<Any?>(symbol, origin, coordinates, annotations, name, kind, visibility, modality, isCompanion, isInner, isData, isExternal, isInline, thisReceiver, typeParameters, declarationContainer, superType)

    fun createIrTypeAlias(symbol : Any, origin : Any, coordinates : Any, annotations : Any, name : Any, visibility : Any, typeParameters : Any, expandedType : Any, isActual : Boolean): Any = arrayOf<Any?>(symbol, origin, coordinates, annotations, name, visibility, typeParameters, expandedType, isActual)

    fun createIrEnumEntry(symbol : Any, origin : Any, coordinates : Any, annotations : Any, initializer : Any?, correspondingClass : Any?, name : Any): Any = arrayOf<Any?>(symbol, origin, coordinates, annotations, initializer, correspondingClass, name)

    fun createIrAnonymousInit(symbol : Any, origin : Any, coordinates : Any, annotations : Any, body : Any): Any = arrayOf<Any?>(symbol, origin, coordinates, annotations, body)

    fun createIrDeclaration_irAnonymousInit(irAnonymousInit : Any): Any = arrayOf<Any?>(irAnonymousInit)
    fun createIrDeclaration_irClass(irClass : Any): Any = arrayOf<Any?>(irClass)
    fun createIrDeclaration_irConstructor(irConstructor : Any): Any = arrayOf<Any?>(irConstructor)
    fun createIrDeclaration_irEnumEntry(irEnumEntry : Any): Any = arrayOf<Any?>(irEnumEntry)
    fun createIrDeclaration_irField(irField : Any): Any = arrayOf<Any?>(irField)
    fun createIrDeclaration_irFunction(irFunction : Any): Any = arrayOf<Any?>(irFunction)
    fun createIrDeclaration_irProperty(irProperty : Any): Any = arrayOf<Any?>(irProperty)
    fun createIrDeclaration_irTypeParameter(irTypeParameter : Any): Any = arrayOf<Any?>(irTypeParameter)
    fun createIrDeclaration_irVariable(irVariable : Any): Any = arrayOf<Any?>(irVariable)
    fun createIrDeclaration_irValueParameter(irValueParameter : Any): Any = arrayOf<Any?>(irValueParameter)
    fun createIrDeclaration_irLocalDelegatedProperty(irLocalDelegatedProperty : Any): Any = arrayOf<Any?>(irLocalDelegatedProperty)
    fun createIrDeclaration_irTypeAlias(irTypeAlias : Any): Any = arrayOf<Any?>(irTypeAlias)

    fun createIrBranch(condition : Any, result : Any): Any = arrayOf<Any?>(condition, result)

    fun createIrBlockBody(statement : List<Any>): Any = arrayOf<Any?>(statement)

    fun createIrCatch(catchParameter : Any, result : Any): Any = arrayOf<Any?>(catchParameter, result)

    fun createIrSyntheticBodyKind(index: Int): Any = index

    fun createIrSyntheticBody(kind : Any): Any = arrayOf<Any?>(kind)

    fun createIrStatement_declaration(coordinates : Any, declaration : Any): Any = arrayOf<Any?>(coordinates, declaration)
    fun createIrStatement_expression(coordinates : Any, expression : Any): Any = arrayOf<Any?>(coordinates, expression)
    fun createIrStatement_blockBody(coordinates : Any, blockBody : Any): Any = arrayOf<Any?>(coordinates, blockBody)
    fun createIrStatement_branch(coordinates : Any, branch : Any): Any = arrayOf<Any?>(coordinates, branch)
    fun createIrStatement_catch(coordinates : Any, catch : Any): Any = arrayOf<Any?>(coordinates, catch)
    fun createIrStatement_syntheticBody(coordinates : Any, syntheticBody : Any): Any = arrayOf<Any?>(coordinates, syntheticBody)

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

    open fun readCoordinates(): Any {
        var startOffset: Int = 0
        var endOffset: Int = 0
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> startOffset = readInt32()
                    2 -> endOffset = readInt32()
                    else -> skip(type)
                }
            }
        }
        return createCoordinates(startOffset, endOffset)
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
        var origin: Any? = null
        var custom: Any? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> {
                        origin = createKnownOrigin(readInt32())
                        oneOfIndex = 1
                    }
                    2 -> {
                        custom = readWithLength { readIrDataIndex() }
                        oneOfIndex = 2
                    }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            1 -> return createIrDeclarationOrigin_origin(origin!!)
            2 -> return createIrDeclarationOrigin_custom(custom!!)
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
        var star: Any? = null
        var type_: Any? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> {
                        star = readWithLength { readIrStarProjection() }
                        oneOfIndex = 1
                    }
                    2 -> {
                        type_ = readWithLength { readIrTypeProjection() }
                        oneOfIndex = 2
                    }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            1 -> return createIrTypeArgument_star(star!!)
            2 -> return createIrTypeArgument_type_(type_!!)
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
        var simple: Any? = null
        var dynamic: Any? = null
        var error: Any? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> {
                        simple = readWithLength { readIrSimpleType() }
                        oneOfIndex = 1
                    }
                    2 -> {
                        dynamic = readWithLength { readIrDynamicType() }
                        oneOfIndex = 2
                    }
                    3 -> {
                        error = readWithLength { readIrErrorType() }
                        oneOfIndex = 3
                    }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            1 -> return createIrType_simple(simple!!)
            2 -> return createIrType_dynamic(dynamic!!)
            3 -> return createIrType_error(error!!)
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

    open fun readMemberAccessCommon(): Any {
        var dispatchReceiver: Any? = null
        var extensionReceiver: Any? = null
        var valueArgument: MutableList<Any> = mutableListOf()
        var typeArguments: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> dispatchReceiver = readWithLength { readIrExpression() }
                    2 -> extensionReceiver = readWithLength { readIrExpression() }
                    3 -> valueArgument.add(readWithLength { readNullableIrExpression() })
                    4 -> typeArguments = readWithLength { readTypeArguments() }
                    else -> skip(type)
                }
            }
        }
        return createMemberAccessCommon(dispatchReceiver, extensionReceiver, valueArgument, typeArguments!!)
    }

    open fun readIrCall(): Any {
        var symbol: Any? = null
        var memberAccess: Any? = null
        var super_: Any? = null
        var origin: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol = readWithLength { readIrDataIndex() }
                    2 -> memberAccess = readWithLength { readMemberAccessCommon() }
                    3 -> super_ = readWithLength { readIrDataIndex() }
                    4 -> origin = readWithLength { readIrStatementOrigin() }
                    else -> skip(type)
                }
            }
        }
        return createIrCall(symbol!!, memberAccess!!, super_, origin)
    }

    open fun readIrConstructorCall(): Any {
        var symbol: Any? = null
        var constructorTypeArgumentsCount: Int = 0
        var memberAccess: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol = readWithLength { readIrDataIndex() }
                    2 -> constructorTypeArgumentsCount = readInt32()
                    3 -> memberAccess = readWithLength { readMemberAccessCommon() }
                    else -> skip(type)
                }
            }
        }
        return createIrConstructorCall(symbol!!, constructorTypeArgumentsCount, memberAccess!!)
    }

    open fun readIrFunctionReference(): Any {
        var symbol: Any? = null
        var origin: Any? = null
        var memberAccess: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol = readWithLength { readIrDataIndex() }
                    2 -> origin = readWithLength { readIrStatementOrigin() }
                    3 -> memberAccess = readWithLength { readMemberAccessCommon() }
                    else -> skip(type)
                }
            }
        }
        return createIrFunctionReference(symbol!!, origin, memberAccess!!)
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
        var memberAccess: Any? = null
        var symbol: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> field = readWithLength { readIrDataIndex() }
                    2 -> getter = readWithLength { readIrDataIndex() }
                    3 -> setter = readWithLength { readIrDataIndex() }
                    4 -> origin = readWithLength { readIrStatementOrigin() }
                    5 -> memberAccess = readWithLength { readMemberAccessCommon() }
                    6 -> symbol = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrPropertyReference(field, getter, setter, origin, memberAccess!!, symbol!!)
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
        var null_: Boolean? = null
        var boolean: Boolean? = null
        var char: Int? = null
        var byte: Int? = null
        var short: Int? = null
        var int: Int? = null
        var long: Long? = null
        var float: Float? = null
        var double: Double? = null
        var string: Any? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> {
                        null_ = readBool()
                        oneOfIndex = 1
                    }
                    2 -> {
                        boolean = readBool()
                        oneOfIndex = 2
                    }
                    3 -> {
                        char = readInt32()
                        oneOfIndex = 3
                    }
                    4 -> {
                        byte = readInt32()
                        oneOfIndex = 4
                    }
                    5 -> {
                        short = readInt32()
                        oneOfIndex = 5
                    }
                    6 -> {
                        int = readInt32()
                        oneOfIndex = 6
                    }
                    7 -> {
                        long = readInt64()
                        oneOfIndex = 7
                    }
                    8 -> {
                        float = readFloat()
                        oneOfIndex = 8
                    }
                    9 -> {
                        double = readDouble()
                        oneOfIndex = 9
                    }
                    10 -> {
                        string = readWithLength { readIrDataIndex() }
                        oneOfIndex = 10
                    }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            1 -> return createIrConst_null_(null_!!)
            2 -> return createIrConst_boolean(boolean!!)
            3 -> return createIrConst_char(char!!)
            4 -> return createIrConst_byte(byte!!)
            5 -> return createIrConst_short(short!!)
            6 -> return createIrConst_int(int!!)
            7 -> return createIrConst_long(long!!)
            8 -> return createIrConst_float(float!!)
            9 -> return createIrConst_double(double!!)
            10 -> return createIrConst_string(string!!)
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
        var memberAccess: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol = readWithLength { readIrDataIndex() }
                    2 -> memberAccess = readWithLength { readMemberAccessCommon() }
                    else -> skip(type)
                }
            }
        }
        return createIrDelegatingConstructorCall(symbol!!, memberAccess!!)
    }

    open fun readIrDoWhile(): Any {
        var loop: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> loop = readWithLength { readLoop() }
                    else -> skip(type)
                }
            }
        }
        return createIrDoWhile(loop!!)
    }

    open fun readIrEnumConstructorCall(): Any {
        var symbol: Any? = null
        var memberAccess: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol = readWithLength { readIrDataIndex() }
                    2 -> memberAccess = readWithLength { readMemberAccessCommon() }
                    else -> skip(type)
                }
            }
        }
        return createIrEnumConstructorCall(symbol!!, memberAccess!!)
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

    open fun readFieldAccessCommon(): Any {
        var symbol: Any? = null
        var super_: Any? = null
        var receiver: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol = readWithLength { readIrDataIndex() }
                    2 -> super_ = readWithLength { readIrDataIndex() }
                    3 -> receiver = readWithLength { readIrExpression() }
                    else -> skip(type)
                }
            }
        }
        return createFieldAccessCommon(symbol!!, super_, receiver)
    }

    open fun readIrGetField(): Any {
        var fieldAccess: Any? = null
        var origin: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> fieldAccess = readWithLength { readFieldAccessCommon() }
                    2 -> origin = readWithLength { readIrStatementOrigin() }
                    else -> skip(type)
                }
            }
        }
        return createIrGetField(fieldAccess!!, origin)
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

    open fun readLoop(): Any {
        var loopId: Int = 0
        var condition: Any? = null
        var label: Any? = null
        var body: Any? = null
        var origin: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> loopId = readInt32()
                    2 -> condition = readWithLength { readIrExpression() }
                    3 -> label = readWithLength { readIrDataIndex() }
                    4 -> body = readWithLength { readIrExpression() }
                    5 -> origin = readWithLength { readIrStatementOrigin() }
                    else -> skip(type)
                }
            }
        }
        return createLoop(loopId, condition!!, label, body, origin)
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
        var fieldAccess: Any? = null
        var value: Any? = null
        var origin: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> fieldAccess = readWithLength { readFieldAccessCommon() }
                    2 -> value = readWithLength { readIrExpression() }
                    3 -> origin = readWithLength { readIrStatementOrigin() }
                    else -> skip(type)
                }
            }
        }
        return createIrSetField(fieldAccess!!, value!!, origin)
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
        var coordinates: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> expression = readWithLength { readIrExpression() }
                    2 -> coordinates = readWithLength { readCoordinates() }
                    else -> skip(type)
                }
            }
        }
        return createIrSpreadElement(expression!!, coordinates!!)
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
        var expression: Any? = null
        var spreadElement: Any? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> {
                        expression = readWithLength { readIrExpression() }
                        oneOfIndex = 1
                    }
                    2 -> {
                        spreadElement = readWithLength { readIrSpreadElement() }
                        oneOfIndex = 2
                    }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            1 -> return createIrVarargElement_expression(expression!!)
            2 -> return createIrVarargElement_spreadElement(spreadElement!!)
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
        var loop: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> loop = readWithLength { readLoop() }
                    else -> skip(type)
                }
            }
        }
        return createIrWhile(loop!!)
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
        var block: Any? = null
        var break_: Any? = null
        var call: Any? = null
        var classReference: Any? = null
        var composite: Any? = null
        var const: Any? = null
        var continue_: Any? = null
        var delegatingConstructorCall: Any? = null
        var doWhile: Any? = null
        var enumConstructorCall: Any? = null
        var functionReference: Any? = null
        var getClass: Any? = null
        var getEnumValue: Any? = null
        var getField: Any? = null
        var getObject: Any? = null
        var getValue: Any? = null
        var instanceInitializerCall: Any? = null
        var propertyReference: Any? = null
        var return_: Any? = null
        var setField: Any? = null
        var setVariable: Any? = null
        var stringConcat: Any? = null
        var throw_: Any? = null
        var try_: Any? = null
        var typeOp: Any? = null
        var vararg: Any? = null
        var when_: Any? = null
        var while_: Any? = null
        var dynamicMember: Any? = null
        var dynamicOperator: Any? = null
        var localDelegatedPropertyReference: Any? = null
        var constructorCall: Any? = null
        var functionExpression: Any? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> {
                        block = readWithLength { readIrBlock() }
                        oneOfIndex = 1
                    }
                    2 -> {
                        break_ = readWithLength { readIrBreak() }
                        oneOfIndex = 2
                    }
                    3 -> {
                        call = readWithLength { readIrCall() }
                        oneOfIndex = 3
                    }
                    4 -> {
                        classReference = readWithLength { readIrClassReference() }
                        oneOfIndex = 4
                    }
                    5 -> {
                        composite = readWithLength { readIrComposite() }
                        oneOfIndex = 5
                    }
                    6 -> {
                        const = readWithLength { readIrConst() }
                        oneOfIndex = 6
                    }
                    7 -> {
                        continue_ = readWithLength { readIrContinue() }
                        oneOfIndex = 7
                    }
                    8 -> {
                        delegatingConstructorCall = readWithLength { readIrDelegatingConstructorCall() }
                        oneOfIndex = 8
                    }
                    9 -> {
                        doWhile = readWithLength { readIrDoWhile() }
                        oneOfIndex = 9
                    }
                    10 -> {
                        enumConstructorCall = readWithLength { readIrEnumConstructorCall() }
                        oneOfIndex = 10
                    }
                    11 -> {
                        functionReference = readWithLength { readIrFunctionReference() }
                        oneOfIndex = 11
                    }
                    12 -> {
                        getClass = readWithLength { readIrGetClass() }
                        oneOfIndex = 12
                    }
                    13 -> {
                        getEnumValue = readWithLength { readIrGetEnumValue() }
                        oneOfIndex = 13
                    }
                    14 -> {
                        getField = readWithLength { readIrGetField() }
                        oneOfIndex = 14
                    }
                    15 -> {
                        getObject = readWithLength { readIrGetObject() }
                        oneOfIndex = 15
                    }
                    16 -> {
                        getValue = readWithLength { readIrGetValue() }
                        oneOfIndex = 16
                    }
                    17 -> {
                        instanceInitializerCall = readWithLength { readIrInstanceInitializerCall() }
                        oneOfIndex = 17
                    }
                    18 -> {
                        propertyReference = readWithLength { readIrPropertyReference() }
                        oneOfIndex = 18
                    }
                    19 -> {
                        return_ = readWithLength { readIrReturn() }
                        oneOfIndex = 19
                    }
                    20 -> {
                        setField = readWithLength { readIrSetField() }
                        oneOfIndex = 20
                    }
                    21 -> {
                        setVariable = readWithLength { readIrSetVariable() }
                        oneOfIndex = 21
                    }
                    22 -> {
                        stringConcat = readWithLength { readIrStringConcat() }
                        oneOfIndex = 22
                    }
                    23 -> {
                        throw_ = readWithLength { readIrThrow() }
                        oneOfIndex = 23
                    }
                    24 -> {
                        try_ = readWithLength { readIrTry() }
                        oneOfIndex = 24
                    }
                    25 -> {
                        typeOp = readWithLength { readIrTypeOp() }
                        oneOfIndex = 25
                    }
                    26 -> {
                        vararg = readWithLength { readIrVararg() }
                        oneOfIndex = 26
                    }
                    27 -> {
                        when_ = readWithLength { readIrWhen() }
                        oneOfIndex = 27
                    }
                    28 -> {
                        while_ = readWithLength { readIrWhile() }
                        oneOfIndex = 28
                    }
                    29 -> {
                        dynamicMember = readWithLength { readIrDynamicMemberExpression() }
                        oneOfIndex = 29
                    }
                    30 -> {
                        dynamicOperator = readWithLength { readIrDynamicOperatorExpression() }
                        oneOfIndex = 30
                    }
                    31 -> {
                        localDelegatedPropertyReference = readWithLength { readIrLocalDelegatedPropertyReference() }
                        oneOfIndex = 31
                    }
                    32 -> {
                        constructorCall = readWithLength { readIrConstructorCall() }
                        oneOfIndex = 32
                    }
                    33 -> {
                        functionExpression = readWithLength { readIrFunctionExpression() }
                        oneOfIndex = 33
                    }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            1 -> return createIrOperation_block(block!!)
            2 -> return createIrOperation_break_(break_!!)
            3 -> return createIrOperation_call(call!!)
            4 -> return createIrOperation_classReference(classReference!!)
            5 -> return createIrOperation_composite(composite!!)
            6 -> return createIrOperation_const(const!!)
            7 -> return createIrOperation_continue_(continue_!!)
            8 -> return createIrOperation_delegatingConstructorCall(delegatingConstructorCall!!)
            9 -> return createIrOperation_doWhile(doWhile!!)
            10 -> return createIrOperation_enumConstructorCall(enumConstructorCall!!)
            11 -> return createIrOperation_functionReference(functionReference!!)
            12 -> return createIrOperation_getClass(getClass!!)
            13 -> return createIrOperation_getEnumValue(getEnumValue!!)
            14 -> return createIrOperation_getField(getField!!)
            15 -> return createIrOperation_getObject(getObject!!)
            16 -> return createIrOperation_getValue(getValue!!)
            17 -> return createIrOperation_instanceInitializerCall(instanceInitializerCall!!)
            18 -> return createIrOperation_propertyReference(propertyReference!!)
            19 -> return createIrOperation_return_(return_!!)
            20 -> return createIrOperation_setField(setField!!)
            21 -> return createIrOperation_setVariable(setVariable!!)
            22 -> return createIrOperation_stringConcat(stringConcat!!)
            23 -> return createIrOperation_throw_(throw_!!)
            24 -> return createIrOperation_try_(try_!!)
            25 -> return createIrOperation_typeOp(typeOp!!)
            26 -> return createIrOperation_vararg(vararg!!)
            27 -> return createIrOperation_when_(when_!!)
            28 -> return createIrOperation_while_(while_!!)
            29 -> return createIrOperation_dynamicMember(dynamicMember!!)
            30 -> return createIrOperation_dynamicOperator(dynamicOperator!!)
            31 -> return createIrOperation_localDelegatedPropertyReference(localDelegatedPropertyReference!!)
            32 -> return createIrOperation_constructorCall(constructorCall!!)
            33 -> return createIrOperation_functionExpression(functionExpression!!)
            else -> error("Incorrect oneOf index: " + oneOfIndex)
        }
    }

    open fun readIrExpression(): Any {
        var operation: Any? = null
        var type_: Any? = null
        var coordinates: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> operation = readWithLength { readIrOperation() }
                    2 -> type_ = readWithLength { readIrDataIndex() }
                    3 -> coordinates = readWithLength { readCoordinates() }
                    else -> skip(type)
                }
            }
        }
        return createIrExpression(operation!!, type_!!, coordinates!!)
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
        var symbol: Any? = null
        var origin: Any? = null
        var coordinates: Any? = null
        var annotations: Any? = null
        var name: Any? = null
        var visibility: Any? = null
        var isInline: Boolean = false
        var isExternal: Boolean = false
        var typeParameters: Any? = null
        var dispatchReceiver: Any? = null
        var extensionReceiver: Any? = null
        var valueParameter: MutableList<Any> = mutableListOf()
        var body: Any? = null
        var returnType: Any? = null
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
                                                    1 -> symbol = readWithLength { readIrDataIndex() }
                                                    2 -> origin = readWithLength { readIrDeclarationOrigin() }
                                                    3 -> coordinates = readWithLength { readCoordinates() }
                                                    4 -> annotations = readWithLength { readAnnotations() }
                                                    else -> skip(type)
                                                }
                                            }
                                        }
                                    }
                                    2 -> name = readWithLength { readIrDataIndex() }
                                    3 -> visibility = readWithLength { readVisibility() }
                                    4 -> isInline = readBool()
                                    5 -> isExternal = readBool()
                                    6 -> typeParameters = readWithLength { readIrTypeParameterContainer() }
                                    7 -> dispatchReceiver = readWithLength { readIrValueParameter() }
                                    8 -> extensionReceiver = readWithLength { readIrValueParameter() }
                                    9 -> valueParameter.add(readWithLength { readIrValueParameter() })
                                    10 -> body = readWithLength { readIrDataIndex() }
                                    11 -> returnType = readWithLength { readIrDataIndex() }
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
        return createIrFunction(symbol!!, origin!!, coordinates!!, annotations!!, name!!, visibility!!, isInline, isExternal, typeParameters!!, dispatchReceiver, extensionReceiver, valueParameter, body, returnType!!, modality!!, isTailrec, isSuspend, overridden)
    }

    open fun readIrConstructor(): Any {
        var symbol: Any? = null
        var origin: Any? = null
        var coordinates: Any? = null
        var annotations: Any? = null
        var name: Any? = null
        var visibility: Any? = null
        var isInline: Boolean = false
        var isExternal: Boolean = false
        var typeParameters: Any? = null
        var dispatchReceiver: Any? = null
        var extensionReceiver: Any? = null
        var valueParameter: MutableList<Any> = mutableListOf()
        var body: Any? = null
        var returnType: Any? = null
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
                                                    1 -> symbol = readWithLength { readIrDataIndex() }
                                                    2 -> origin = readWithLength { readIrDeclarationOrigin() }
                                                    3 -> coordinates = readWithLength { readCoordinates() }
                                                    4 -> annotations = readWithLength { readAnnotations() }
                                                    else -> skip(type)
                                                }
                                            }
                                        }
                                    }
                                    2 -> name = readWithLength { readIrDataIndex() }
                                    3 -> visibility = readWithLength { readVisibility() }
                                    4 -> isInline = readBool()
                                    5 -> isExternal = readBool()
                                    6 -> typeParameters = readWithLength { readIrTypeParameterContainer() }
                                    7 -> dispatchReceiver = readWithLength { readIrValueParameter() }
                                    8 -> extensionReceiver = readWithLength { readIrValueParameter() }
                                    9 -> valueParameter.add(readWithLength { readIrValueParameter() })
                                    10 -> body = readWithLength { readIrDataIndex() }
                                    11 -> returnType = readWithLength { readIrDataIndex() }
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
        return createIrConstructor(symbol!!, origin!!, coordinates!!, annotations!!, name!!, visibility!!, isInline, isExternal, typeParameters!!, dispatchReceiver, extensionReceiver, valueParameter, body, returnType!!, isPrimary)
    }

    open fun readIrField(): Any {
        var symbol: Any? = null
        var origin: Any? = null
        var coordinates: Any? = null
        var annotations: Any? = null
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
                                    1 -> symbol = readWithLength { readIrDataIndex() }
                                    2 -> origin = readWithLength { readIrDeclarationOrigin() }
                                    3 -> coordinates = readWithLength { readCoordinates() }
                                    4 -> annotations = readWithLength { readAnnotations() }
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
        return createIrField(symbol!!, origin!!, coordinates!!, annotations!!, initializer, name!!, visibility!!, isFinal, isExternal, isStatic, type_!!)
    }

    open fun readIrLocalDelegatedProperty(): Any {
        var symbol: Any? = null
        var origin: Any? = null
        var coordinates: Any? = null
        var annotations: Any? = null
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
                                    1 -> symbol = readWithLength { readIrDataIndex() }
                                    2 -> origin = readWithLength { readIrDeclarationOrigin() }
                                    3 -> coordinates = readWithLength { readCoordinates() }
                                    4 -> annotations = readWithLength { readAnnotations() }
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
        return createIrLocalDelegatedProperty(symbol!!, origin!!, coordinates!!, annotations!!, name!!, type_!!, isVar, delegate!!, getter, setter)
    }

    open fun readIrProperty(): Any {
        var symbol: Any? = null
        var origin: Any? = null
        var coordinates: Any? = null
        var annotations: Any? = null
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
                                    1 -> symbol = readWithLength { readIrDataIndex() }
                                    2 -> origin = readWithLength { readIrDeclarationOrigin() }
                                    3 -> coordinates = readWithLength { readCoordinates() }
                                    4 -> annotations = readWithLength { readAnnotations() }
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
        return createIrProperty(symbol!!, origin!!, coordinates!!, annotations!!, name!!, visibility!!, modality!!, isVar, isConst, isLateinit, isDelegated, isExternal, backingField, getter, setter)
    }

    open fun readIrVariable(): Any {
        var symbol: Any? = null
        var origin: Any? = null
        var coordinates: Any? = null
        var annotations: Any? = null
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
                                    1 -> symbol = readWithLength { readIrDataIndex() }
                                    2 -> origin = readWithLength { readIrDeclarationOrigin() }
                                    3 -> coordinates = readWithLength { readCoordinates() }
                                    4 -> annotations = readWithLength { readAnnotations() }
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
        return createIrVariable(symbol!!, origin!!, coordinates!!, annotations!!, name!!, type_!!, isVar, isConst, isLateinit, initializer)
    }

    open fun readIrValueParameter(): Any {
        var symbol: Any? = null
        var origin: Any? = null
        var coordinates: Any? = null
        var annotations: Any? = null
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
                                    1 -> symbol = readWithLength { readIrDataIndex() }
                                    2 -> origin = readWithLength { readIrDeclarationOrigin() }
                                    3 -> coordinates = readWithLength { readCoordinates() }
                                    4 -> annotations = readWithLength { readAnnotations() }
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
        return createIrValueParameter(symbol!!, origin!!, coordinates!!, annotations!!, name!!, index, type_!!, varargElementType, isCrossinline, isNoinline, defaultValue)
    }

    open fun readIrTypeParameter(): Any {
        var symbol: Any? = null
        var origin: Any? = null
        var coordinates: Any? = null
        var annotations: Any? = null
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
                                    1 -> symbol = readWithLength { readIrDataIndex() }
                                    2 -> origin = readWithLength { readIrDeclarationOrigin() }
                                    3 -> coordinates = readWithLength { readCoordinates() }
                                    4 -> annotations = readWithLength { readAnnotations() }
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
        return createIrTypeParameter(symbol!!, origin!!, coordinates!!, annotations!!, name!!, index, variance!!, superType, isReified)
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
        var symbol: Any? = null
        var origin: Any? = null
        var coordinates: Any? = null
        var annotations: Any? = null
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
        var typeParameters: Any? = null
        var declarationContainer: Any? = null
        var superType: MutableList<Any> = mutableListOf()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> symbol = readWithLength { readIrDataIndex() }
                                    2 -> origin = readWithLength { readIrDeclarationOrigin() }
                                    3 -> coordinates = readWithLength { readCoordinates() }
                                    4 -> annotations = readWithLength { readAnnotations() }
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
                    11 -> thisReceiver = readWithLength { readIrValueParameter() }
                    12 -> typeParameters = readWithLength { readIrTypeParameterContainer() }
                    13 -> declarationContainer = readWithLength { readIrDeclarationContainer() }
                    14 -> superType.add(readWithLength { readIrDataIndex() })
                    else -> skip(type)
                }
            }
        }
        return createIrClass(symbol!!, origin!!, coordinates!!, annotations!!, name!!, kind!!, visibility!!, modality!!, isCompanion, isInner, isData, isExternal, isInline, thisReceiver, typeParameters!!, declarationContainer!!, superType)
    }

    open fun readIrTypeAlias(): Any {
        var symbol: Any? = null
        var origin: Any? = null
        var coordinates: Any? = null
        var annotations: Any? = null
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
                                    1 -> symbol = readWithLength { readIrDataIndex() }
                                    2 -> origin = readWithLength { readIrDeclarationOrigin() }
                                    3 -> coordinates = readWithLength { readCoordinates() }
                                    4 -> annotations = readWithLength { readAnnotations() }
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
        return createIrTypeAlias(symbol!!, origin!!, coordinates!!, annotations!!, name!!, visibility!!, typeParameters!!, expandedType!!, isActual)
    }

    open fun readIrEnumEntry(): Any {
        var symbol: Any? = null
        var origin: Any? = null
        var coordinates: Any? = null
        var annotations: Any? = null
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
                                    1 -> symbol = readWithLength { readIrDataIndex() }
                                    2 -> origin = readWithLength { readIrDeclarationOrigin() }
                                    3 -> coordinates = readWithLength { readCoordinates() }
                                    4 -> annotations = readWithLength { readAnnotations() }
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
        return createIrEnumEntry(symbol!!, origin!!, coordinates!!, annotations!!, initializer, correspondingClass, name!!)
    }

    open fun readIrAnonymousInit(): Any {
        var symbol: Any? = null
        var origin: Any? = null
        var coordinates: Any? = null
        var annotations: Any? = null
        var body: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> readWithLength {
                        while (hasData) {
                            readField { fieldNumber, type ->
                                when (fieldNumber) {
                                    1 -> symbol = readWithLength { readIrDataIndex() }
                                    2 -> origin = readWithLength { readIrDeclarationOrigin() }
                                    3 -> coordinates = readWithLength { readCoordinates() }
                                    4 -> annotations = readWithLength { readAnnotations() }
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
        return createIrAnonymousInit(symbol!!, origin!!, coordinates!!, annotations!!, body!!)
    }

    open fun readIrDeclaration(): Any {
        var irAnonymousInit: Any? = null
        var irClass: Any? = null
        var irConstructor: Any? = null
        var irEnumEntry: Any? = null
        var irField: Any? = null
        var irFunction: Any? = null
        var irProperty: Any? = null
        var irTypeParameter: Any? = null
        var irVariable: Any? = null
        var irValueParameter: Any? = null
        var irLocalDelegatedProperty: Any? = null
        var irTypeAlias: Any? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> {
                        irAnonymousInit = readWithLength { readIrAnonymousInit() }
                        oneOfIndex = 1
                    }
                    2 -> {
                        irClass = readWithLength { readIrClass() }
                        oneOfIndex = 2
                    }
                    3 -> {
                        irConstructor = readWithLength { readIrConstructor() }
                        oneOfIndex = 3
                    }
                    4 -> {
                        irEnumEntry = readWithLength { readIrEnumEntry() }
                        oneOfIndex = 4
                    }
                    5 -> {
                        irField = readWithLength { readIrField() }
                        oneOfIndex = 5
                    }
                    6 -> {
                        irFunction = readWithLength { readIrFunction() }
                        oneOfIndex = 6
                    }
                    7 -> {
                        irProperty = readWithLength { readIrProperty() }
                        oneOfIndex = 7
                    }
                    8 -> {
                        irTypeParameter = readWithLength { readIrTypeParameter() }
                        oneOfIndex = 8
                    }
                    9 -> {
                        irVariable = readWithLength { readIrVariable() }
                        oneOfIndex = 9
                    }
                    10 -> {
                        irValueParameter = readWithLength { readIrValueParameter() }
                        oneOfIndex = 10
                    }
                    11 -> {
                        irLocalDelegatedProperty = readWithLength { readIrLocalDelegatedProperty() }
                        oneOfIndex = 11
                    }
                    12 -> {
                        irTypeAlias = readWithLength { readIrTypeAlias() }
                        oneOfIndex = 12
                    }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            1 -> return createIrDeclaration_irAnonymousInit(irAnonymousInit!!)
            2 -> return createIrDeclaration_irClass(irClass!!)
            3 -> return createIrDeclaration_irConstructor(irConstructor!!)
            4 -> return createIrDeclaration_irEnumEntry(irEnumEntry!!)
            5 -> return createIrDeclaration_irField(irField!!)
            6 -> return createIrDeclaration_irFunction(irFunction!!)
            7 -> return createIrDeclaration_irProperty(irProperty!!)
            8 -> return createIrDeclaration_irTypeParameter(irTypeParameter!!)
            9 -> return createIrDeclaration_irVariable(irVariable!!)
            10 -> return createIrDeclaration_irValueParameter(irValueParameter!!)
            11 -> return createIrDeclaration_irLocalDelegatedProperty(irLocalDelegatedProperty!!)
            12 -> return createIrDeclaration_irTypeAlias(irTypeAlias!!)
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

    open fun readIrStatement(): Any {
        var coordinates: Any? = null
        var declaration: Any? = null
        var expression: Any? = null
        var blockBody: Any? = null
        var branch: Any? = null
        var catch: Any? = null
        var syntheticBody: Any? = null
        var oneOfIndex: Int = -1
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> coordinates = readWithLength { readCoordinates() }
                    2 -> {
                        declaration = readWithLength { readIrDeclaration() }
                        oneOfIndex = 2
                    }
                    3 -> {
                        expression = readWithLength { readIrExpression() }
                        oneOfIndex = 3
                    }
                    4 -> {
                        blockBody = readWithLength { readIrBlockBody() }
                        oneOfIndex = 4
                    }
                    5 -> {
                        branch = readWithLength { readIrBranch() }
                        oneOfIndex = 5
                    }
                    6 -> {
                        catch = readWithLength { readIrCatch() }
                        oneOfIndex = 6
                    }
                    7 -> {
                        syntheticBody = readWithLength { readIrSyntheticBody() }
                        oneOfIndex = 7
                    }
                    else -> skip(type)
                }
            }
        }
        when (oneOfIndex) {
            2 -> return createIrStatement_declaration(coordinates!!, declaration!!)
            3 -> return createIrStatement_expression(coordinates!!, expression!!)
            4 -> return createIrStatement_blockBody(coordinates!!, blockBody!!)
            5 -> return createIrStatement_branch(coordinates!!, branch!!)
            6 -> return createIrStatement_catch(coordinates!!, catch!!)
            7 -> return createIrStatement_syntheticBody(coordinates!!, syntheticBody!!)
            else -> error("Incorrect oneOf index: " + oneOfIndex)
        }
    }

}