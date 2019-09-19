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

    fun createIrDeclarationOrigin(origin : Any?, custom : Any?): Any = arrayOf<Any?>(origin, custom)

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

    fun createIrTypeProjection(variance : Any, type : Any): Any = arrayOf<Any?>(variance, type)

    fun createIrTypeArgument(star : Any?, type : Any?): Any = arrayOf<Any?>(star, type)

    fun createIrSimpleType(annotations : Any, classifier : Any, hasQuestionMark : Boolean, argument : List<Any>, abbreviation : Any?): Any = arrayOf<Any?>(annotations, classifier, hasQuestionMark, argument, abbreviation)

    fun createIrTypeAbbreviation(annotations : Any, typeAlias : Any, hasQuestionMark : Boolean, argument : List<Any>): Any = arrayOf<Any?>(annotations, typeAlias, hasQuestionMark, argument)

    fun createIrDynamicType(annotations : Any): Any = arrayOf<Any?>(annotations)

    fun createIrErrorType(annotations : Any): Any = arrayOf<Any?>(annotations)

    fun createIrType(simple : Any?, dynamic : Any?, error : Any?): Any = arrayOf<Any?>(simple, dynamic, error)

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

    fun createIrConst(null_ : Boolean?, boolean : Boolean?, char : Int?, byte : Int?, short : Int?, int : Int?, long : Long?, float : Float?, double : Double?, string : Any?): Any = arrayOf<Any?>(null_, boolean, char, byte, short, int, long, float, double, string)

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

    fun createIrVarargElement(expression : Any?, spreadElement : Any?): Any = arrayOf<Any?>(expression, spreadElement)

    fun createIrWhen(branch : List<Any>, origin : Any?): Any = arrayOf<Any?>(branch, origin)

    fun createIrWhile(loop : Any): Any = arrayOf<Any?>(loop)

    fun createIrFunctionExpression(function : Any, origin : Any): Any = arrayOf<Any?>(function, origin)

    fun createIrDynamicMemberExpression(memberName : Any, receiver : Any): Any = arrayOf<Any?>(memberName, receiver)

    fun createIrDynamicOperator(index: Int): Any = index

    fun createIrDynamicOperatorExpression(operator : Any, receiver : Any, argument : List<Any>): Any = arrayOf<Any?>(operator, receiver, argument)

    fun createIrOperation(block : Any?, break_ : Any?, call : Any?, classReference : Any?, composite : Any?, const : Any?, continue_ : Any?, delegatingConstructorCall : Any?, doWhile : Any?, enumConstructorCall : Any?, functionReference : Any?, getClass : Any?, getEnumValue : Any?, getField : Any?, getObject : Any?, getValue : Any?, instanceInitializerCall : Any?, propertyReference : Any?, return_ : Any?, setField : Any?, setVariable : Any?, stringConcat : Any?, throw_ : Any?, try_ : Any?, typeOp : Any?, vararg : Any?, when_ : Any?, while_ : Any?, dynamicMember : Any?, dynamicOperator : Any?, localDelegatedPropertyReference : Any?, constructorCall : Any?, functionExpression : Any?): Any = arrayOf<Any?>(block, break_, call, classReference, composite, const, continue_, delegatingConstructorCall, doWhile, enumConstructorCall, functionReference, getClass, getEnumValue, getField, getObject, getValue, instanceInitializerCall, propertyReference, return_, setField, setVariable, stringConcat, throw_, try_, typeOp, vararg, when_, while_, dynamicMember, dynamicOperator, localDelegatedPropertyReference, constructorCall, functionExpression)

    fun createIrTypeOperator(index: Int): Any = index

    fun createIrExpression(operation : Any, type : Any, coordinates : Any): Any = arrayOf<Any?>(operation, type, coordinates)

    fun createNullableIrExpression(expression : Any?): Any = arrayOf<Any?>(expression)

    fun createIrDeclarationBase(symbol : Any, origin : Any, coordinates : Any, annotations : Any): Any = arrayOf<Any?>(symbol, origin, coordinates, annotations)

    fun createIrFunctionBase(base : Any, name : Any, visibility : Any, isInline : Boolean, isExternal : Boolean, typeParameters : Any, dispatchReceiver : Any?, extensionReceiver : Any?, valueParameter : List<Any>, body : Any?, returnType : Any): Any = arrayOf<Any?>(base, name, visibility, isInline, isExternal, typeParameters, dispatchReceiver, extensionReceiver, valueParameter, body, returnType)

    fun createIrFunction(base : Any, modality : Any, isTailrec : Boolean, isSuspend : Boolean, overridden : List<Any>): Any = arrayOf<Any?>(base, modality, isTailrec, isSuspend, overridden)

    fun createIrConstructor(base : Any, isPrimary : Boolean): Any = arrayOf<Any?>(base, isPrimary)

    fun createIrField(base : Any, initializer : Any?, name : Any, visibility : Any, isFinal : Boolean, isExternal : Boolean, isStatic : Boolean, type : Any): Any = arrayOf<Any?>(base, initializer, name, visibility, isFinal, isExternal, isStatic, type)

    fun createIrLocalDelegatedProperty(base : Any, name : Any, type : Any, isVar : Boolean, delegate : Any, getter : Any?, setter : Any?): Any = arrayOf<Any?>(base, name, type, isVar, delegate, getter, setter)

    fun createIrProperty(base : Any, name : Any, visibility : Any, modality : Any, isVar : Boolean, isConst : Boolean, isLateinit : Boolean, isDelegated : Boolean, isExternal : Boolean, backingField : Any?, getter : Any?, setter : Any?): Any = arrayOf<Any?>(base, name, visibility, modality, isVar, isConst, isLateinit, isDelegated, isExternal, backingField, getter, setter)

    fun createIrVariable(base : Any, name : Any, type : Any, isVar : Boolean, isConst : Boolean, isLateinit : Boolean, initializer : Any?): Any = arrayOf<Any?>(base, name, type, isVar, isConst, isLateinit, initializer)

    fun createClassKind(index: Int): Any = index

    fun createModalityKind(index: Int): Any = index

    fun createIrValueParameter(base : Any, name : Any, index : Int, type : Any, varargElementType : Any?, isCrossinline : Boolean, isNoinline : Boolean, defaultValue : Any?): Any = arrayOf<Any?>(base, name, index, type, varargElementType, isCrossinline, isNoinline, defaultValue)

    fun createIrTypeParameter(base : Any, name : Any, index : Int, variance : Any, superType : List<Any>, isReified : Boolean): Any = arrayOf<Any?>(base, name, index, variance, superType, isReified)

    fun createIrTypeParameterContainer(typeParameter : List<Any>): Any = arrayOf<Any?>(typeParameter)

    fun createIrClass(base : Any, name : Any, kind : Any, visibility : Any, modality : Any, isCompanion : Boolean, isInner : Boolean, isData : Boolean, isExternal : Boolean, isInline : Boolean, thisReceiver : Any?, typeParameters : Any, declarationContainer : Any, superType : List<Any>): Any = arrayOf<Any?>(base, name, kind, visibility, modality, isCompanion, isInner, isData, isExternal, isInline, thisReceiver, typeParameters, declarationContainer, superType)

    fun createIrTypeAlias(base : Any, name : Any, visibility : Any, typeParameters : Any, expandedType : Any, isActual : Boolean): Any = arrayOf<Any?>(base, name, visibility, typeParameters, expandedType, isActual)

    fun createIrEnumEntry(base : Any, initializer : Any?, correspondingClass : Any?, name : Any): Any = arrayOf<Any?>(base, initializer, correspondingClass, name)

    fun createIrAnonymousInit(base : Any, body : Any): Any = arrayOf<Any?>(base, body)

    fun createIrDeclaration(irAnonymousInit : Any?, irClass : Any?, irConstructor : Any?, irEnumEntry : Any?, irField : Any?, irFunction : Any?, irProperty : Any?, irTypeParameter : Any?, irVariable : Any?, irValueParameter : Any?, irLocalDelegatedProperty : Any?, irTypeAlias : Any?): Any = arrayOf<Any?>(irAnonymousInit, irClass, irConstructor, irEnumEntry, irField, irFunction, irProperty, irTypeParameter, irVariable, irValueParameter, irLocalDelegatedProperty, irTypeAlias)

    fun createIrBranch(condition : Any, result : Any): Any = arrayOf<Any?>(condition, result)

    fun createIrBlockBody(statement : List<Any>): Any = arrayOf<Any?>(statement)

    fun createIrCatch(catchParameter : Any, result : Any): Any = arrayOf<Any?>(catchParameter, result)

    fun createIrSyntheticBodyKind(index: Int): Any = index

    fun createIrSyntheticBody(kind : Any): Any = arrayOf<Any?>(kind)

    fun createIrStatement(coordinates : Any, declaration : Any?, expression : Any?, blockBody : Any?, branch : Any?, catch : Any?, syntheticBody : Any?): Any = arrayOf<Any?>(coordinates, declaration, expression, blockBody, branch, catch, syntheticBody)

    open fun readDescriptorReference(): Any {
        var package_fq_name__: Any? = null
        var class_fq_name__: Any? = null
        var name__: Any? = null
        var uniq_id__: Any? = null
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

    open fun readUniqId(): Any {
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

    open fun readCoordinates(): Any {
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

    open fun readVisibility(): Any {
        var name__: Any? = null
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

    open fun readIrStatementOrigin(): Any {
        var name__: Any? = null
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

    open fun readIrDeclarationOrigin(): Any {
        var origin__: Any? = null
        var custom__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> origin__ = createKnownOrigin(readInt32())
                    2 -> custom__ = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrDeclarationOrigin(origin__, custom__)
    }

    open fun readIrDataIndex(): Any {
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

    open fun readFqName(): Any {
        var segment__: MutableList<Any> = mutableListOf()
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

    open fun readIrDeclarationContainer(): Any {
        var declaration__: MutableList<Any> = mutableListOf()
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

    open fun readFileEntry(): Any {
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

    open fun readIrFile(): Any {
        var declaration_id__: MutableList<Any> = mutableListOf()
        var file_entry__: Any? = null
        var fq_name__: Any? = null
        var annotations__: Any? = null
        var explicitly_exported_to_compiler__: MutableList<Any> = mutableListOf()
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

    open fun readStringTable(): Any {
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

    open fun readIrSymbolData(): Any {
        var kind__: Any? = null
        var uniq_id__: Any? = null
        var top_level_uniq_id__: Any? = null
        var fqname__: Any? = null
        var descriptor_reference__: Any? = null
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

    open fun readIrSymbolTable(): Any {
        var symbols__: MutableList<Any> = mutableListOf()
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

    open fun readAnnotations(): Any {
        var annotation__: MutableList<Any> = mutableListOf()
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

    open fun readTypeArguments(): Any {
        var type_argument__: MutableList<Any> = mutableListOf()
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

    open fun readIrStarProjection(): Any {
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

    open fun readIrTypeProjection(): Any {
        var variance__: Any? = null
        var type__: Any? = null
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

    open fun readIrTypeArgument(): Any {
        var star__: Any? = null
        var type__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> star__ = readWithLength { readIrStarProjection() }
                    2 -> type__ = readWithLength { readIrTypeProjection() }
                    else -> skip(type)
                }
            }
        }
        return createIrTypeArgument(star__, type__)
    }

    open fun readIrSimpleType(): Any {
        var annotations__: Any? = null
        var classifier__: Any? = null
        var has_question_mark__: Boolean = false
        var argument__: MutableList<Any> = mutableListOf()
        var abbreviation__: Any? = null
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

    open fun readIrTypeAbbreviation(): Any {
        var annotations__: Any? = null
        var type_alias__: Any? = null
        var has_question_mark__: Boolean = false
        var argument__: MutableList<Any> = mutableListOf()
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

    open fun readIrDynamicType(): Any {
        var annotations__: Any? = null
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

    open fun readIrErrorType(): Any {
        var annotations__: Any? = null
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

    open fun readIrType(): Any {
        var simple__: Any? = null
        var dynamic__: Any? = null
        var error__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> simple__ = readWithLength { readIrSimpleType() }
                    2 -> dynamic__ = readWithLength { readIrDynamicType() }
                    3 -> error__ = readWithLength { readIrErrorType() }
                    else -> skip(type)
                }
            }
        }
        return createIrType(simple__, dynamic__, error__)
    }

    open fun readIrTypeTable(): Any {
        var types__: MutableList<Any> = mutableListOf()
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

    open fun readIrBreak(): Any {
        var loop_id__: Int = 0
        var label__: Any? = null
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

    open fun readIrBlock(): Any {
        var origin__: Any? = null
        var statement__: MutableList<Any> = mutableListOf()
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

    open fun readMemberAccessCommon(): Any {
        var dispatch_receiver__: Any? = null
        var extension_receiver__: Any? = null
        var value_argument__: MutableList<Any> = mutableListOf()
        var type_arguments__: Any? = null
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

    open fun readIrCall(): Any {
        var symbol__: Any? = null
        var member_access__: Any? = null
        var super__: Any? = null
        var origin__: Any? = null
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

    open fun readIrConstructorCall(): Any {
        var symbol__: Any? = null
        var constructor_type_arguments_count__: Int = 0
        var member_access__: Any? = null
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

    open fun readIrFunctionReference(): Any {
        var symbol__: Any? = null
        var origin__: Any? = null
        var member_access__: Any? = null
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

    open fun readIrLocalDelegatedPropertyReference(): Any {
        var delegate__: Any? = null
        var getter__: Any? = null
        var setter__: Any? = null
        var symbol__: Any? = null
        var origin__: Any? = null
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

    open fun readIrPropertyReference(): Any {
        var field__: Any? = null
        var getter__: Any? = null
        var setter__: Any? = null
        var origin__: Any? = null
        var member_access__: Any? = null
        var symbol__: Any? = null
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

    open fun readIrComposite(): Any {
        var statement__: MutableList<Any> = mutableListOf()
        var origin__: Any? = null
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

    open fun readIrClassReference(): Any {
        var class_symbol__: Any? = null
        var class_type__: Any? = null
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

    open fun readIrConst(): Any {
        var null__: Boolean? = null
        var boolean__: Boolean? = null
        var char__: Int? = null
        var byte__: Int? = null
        var short__: Int? = null
        var int__: Int? = null
        var long__: Long? = null
        var float__: Float? = null
        var double__: Double? = null
        var string__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> null__ = readBool()
                    2 -> boolean__ = readBool()
                    3 -> char__ = readInt32()
                    4 -> byte__ = readInt32()
                    5 -> short__ = readInt32()
                    6 -> int__ = readInt32()
                    7 -> long__ = readInt64()
                    8 -> float__ = readFloat()
                    9 -> double__ = readDouble()
                    10 -> string__ = readWithLength { readIrDataIndex() }
                    else -> skip(type)
                }
            }
        }
        return createIrConst(null__, boolean__, char__, byte__, short__, int__, long__, float__, double__, string__)
    }

    open fun readIrContinue(): Any {
        var loop_id__: Int = 0
        var label__: Any? = null
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

    open fun readIrDelegatingConstructorCall(): Any {
        var symbol__: Any? = null
        var member_access__: Any? = null
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

    open fun readIrDoWhile(): Any {
        var loop__: Any? = null
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

    open fun readIrEnumConstructorCall(): Any {
        var symbol__: Any? = null
        var member_access__: Any? = null
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

    open fun readIrGetClass(): Any {
        var argument__: Any? = null
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

    open fun readIrGetEnumValue(): Any {
        var symbol__: Any? = null
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

    open fun readFieldAccessCommon(): Any {
        var symbol__: Any? = null
        var super__: Any? = null
        var receiver__: Any? = null
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

    open fun readIrGetField(): Any {
        var field_access__: Any? = null
        var origin__: Any? = null
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

    open fun readIrGetValue(): Any {
        var symbol__: Any? = null
        var origin__: Any? = null
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

    open fun readIrGetObject(): Any {
        var symbol__: Any? = null
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

    open fun readIrInstanceInitializerCall(): Any {
        var symbol__: Any? = null
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

    open fun readLoop(): Any {
        var loop_id__: Int = 0
        var condition__: Any? = null
        var label__: Any? = null
        var body__: Any? = null
        var origin__: Any? = null
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

    open fun readIrReturn(): Any {
        var return_target__: Any? = null
        var value__: Any? = null
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

    open fun readIrSetField(): Any {
        var field_access__: Any? = null
        var value__: Any? = null
        var origin__: Any? = null
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

    open fun readIrSetVariable(): Any {
        var symbol__: Any? = null
        var value__: Any? = null
        var origin__: Any? = null
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

    open fun readIrSpreadElement(): Any {
        var expression__: Any? = null
        var coordinates__: Any? = null
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

    open fun readIrStringConcat(): Any {
        var argument__: MutableList<Any> = mutableListOf()
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

    open fun readIrThrow(): Any {
        var value__: Any? = null
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

    open fun readIrTry(): Any {
        var result__: Any? = null
        var catch__: MutableList<Any> = mutableListOf()
        var finally__: Any? = null
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

    open fun readIrTypeOp(): Any {
        var operator__: Any? = null
        var operand__: Any? = null
        var argument__: Any? = null
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

    open fun readIrVararg(): Any {
        var element_type__: Any? = null
        var element__: MutableList<Any> = mutableListOf()
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

    open fun readIrVarargElement(): Any {
        var expression__: Any? = null
        var spread_element__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> expression__ = readWithLength { readIrExpression() }
                    2 -> spread_element__ = readWithLength { readIrSpreadElement() }
                    else -> skip(type)
                }
            }
        }
        return createIrVarargElement(expression__, spread_element__)
    }

    open fun readIrWhen(): Any {
        var branch__: MutableList<Any> = mutableListOf()
        var origin__: Any? = null
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

    open fun readIrWhile(): Any {
        var loop__: Any? = null
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

    open fun readIrFunctionExpression(): Any {
        var function__: Any? = null
        var origin__: Any? = null
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

    open fun readIrDynamicMemberExpression(): Any {
        var memberName__: Any? = null
        var receiver__: Any? = null
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

    open fun readIrDynamicOperatorExpression(): Any {
        var operator__: Any? = null
        var receiver__: Any? = null
        var argument__: MutableList<Any> = mutableListOf()
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

    open fun readIrOperation(): Any {
        var block__: Any? = null
        var break__: Any? = null
        var call__: Any? = null
        var class_reference__: Any? = null
        var composite__: Any? = null
        var const__: Any? = null
        var continue__: Any? = null
        var delegating_constructor_call__: Any? = null
        var do_while__: Any? = null
        var enum_constructor_call__: Any? = null
        var function_reference__: Any? = null
        var get_class__: Any? = null
        var get_enum_value__: Any? = null
        var get_field__: Any? = null
        var get_object__: Any? = null
        var get_value__: Any? = null
        var instance_initializer_call__: Any? = null
        var property_reference__: Any? = null
        var return__: Any? = null
        var set_field__: Any? = null
        var set_variable__: Any? = null
        var string_concat__: Any? = null
        var throw__: Any? = null
        var try__: Any? = null
        var type_op__: Any? = null
        var vararg__: Any? = null
        var when__: Any? = null
        var while__: Any? = null
        var dynamic_member__: Any? = null
        var dynamic_operator__: Any? = null
        var local_delegated_property_reference__: Any? = null
        var constructor_call__: Any? = null
        var function_expression__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> block__ = readWithLength { readIrBlock() }
                    2 -> break__ = readWithLength { readIrBreak() }
                    3 -> call__ = readWithLength { readIrCall() }
                    4 -> class_reference__ = readWithLength { readIrClassReference() }
                    5 -> composite__ = readWithLength { readIrComposite() }
                    6 -> const__ = readWithLength { readIrConst() }
                    7 -> continue__ = readWithLength { readIrContinue() }
                    8 -> delegating_constructor_call__ = readWithLength { readIrDelegatingConstructorCall() }
                    9 -> do_while__ = readWithLength { readIrDoWhile() }
                    10 -> enum_constructor_call__ = readWithLength { readIrEnumConstructorCall() }
                    11 -> function_reference__ = readWithLength { readIrFunctionReference() }
                    12 -> get_class__ = readWithLength { readIrGetClass() }
                    13 -> get_enum_value__ = readWithLength { readIrGetEnumValue() }
                    14 -> get_field__ = readWithLength { readIrGetField() }
                    15 -> get_object__ = readWithLength { readIrGetObject() }
                    16 -> get_value__ = readWithLength { readIrGetValue() }
                    17 -> instance_initializer_call__ = readWithLength { readIrInstanceInitializerCall() }
                    18 -> property_reference__ = readWithLength { readIrPropertyReference() }
                    19 -> return__ = readWithLength { readIrReturn() }
                    20 -> set_field__ = readWithLength { readIrSetField() }
                    21 -> set_variable__ = readWithLength { readIrSetVariable() }
                    22 -> string_concat__ = readWithLength { readIrStringConcat() }
                    23 -> throw__ = readWithLength { readIrThrow() }
                    24 -> try__ = readWithLength { readIrTry() }
                    25 -> type_op__ = readWithLength { readIrTypeOp() }
                    26 -> vararg__ = readWithLength { readIrVararg() }
                    27 -> when__ = readWithLength { readIrWhen() }
                    28 -> while__ = readWithLength { readIrWhile() }
                    29 -> dynamic_member__ = readWithLength { readIrDynamicMemberExpression() }
                    30 -> dynamic_operator__ = readWithLength { readIrDynamicOperatorExpression() }
                    31 -> local_delegated_property_reference__ = readWithLength { readIrLocalDelegatedPropertyReference() }
                    32 -> constructor_call__ = readWithLength { readIrConstructorCall() }
                    33 -> function_expression__ = readWithLength { readIrFunctionExpression() }
                    else -> skip(type)
                }
            }
        }
        return createIrOperation(block__, break__, call__, class_reference__, composite__, const__, continue__, delegating_constructor_call__, do_while__, enum_constructor_call__, function_reference__, get_class__, get_enum_value__, get_field__, get_object__, get_value__, instance_initializer_call__, property_reference__, return__, set_field__, set_variable__, string_concat__, throw__, try__, type_op__, vararg__, when__, while__, dynamic_member__, dynamic_operator__, local_delegated_property_reference__, constructor_call__, function_expression__)
    }

    open fun readIrExpression(): Any {
        var operation__: Any? = null
        var type__: Any? = null
        var coordinates__: Any? = null
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

    open fun readNullableIrExpression(): Any {
        var expression__: Any? = null
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

    open fun readIrDeclarationBase(): Any {
        var symbol__: Any? = null
        var origin__: Any? = null
        var coordinates__: Any? = null
        var annotations__: Any? = null
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

    open fun readIrFunctionBase(): Any {
        var base__: Any? = null
        var name__: Any? = null
        var visibility__: Any? = null
        var is_inline__: Boolean = false
        var is_external__: Boolean = false
        var type_parameters__: Any? = null
        var dispatch_receiver__: Any? = null
        var extension_receiver__: Any? = null
        var value_parameter__: MutableList<Any> = mutableListOf()
        var body__: Any? = null
        var return_type__: Any? = null
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

    open fun readIrFunction(): Any {
        var base__: Any? = null
        var modality__: Any? = null
        var is_tailrec__: Boolean = false
        var is_suspend__: Boolean = false
        var overridden__: MutableList<Any> = mutableListOf()
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

    open fun readIrConstructor(): Any {
        var base__: Any? = null
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

    open fun readIrField(): Any {
        var base__: Any? = null
        var initializer__: Any? = null
        var name__: Any? = null
        var visibility__: Any? = null
        var is_final__: Boolean = false
        var is_external__: Boolean = false
        var is_static__: Boolean = false
        var type__: Any? = null
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

    open fun readIrLocalDelegatedProperty(): Any {
        var base__: Any? = null
        var name__: Any? = null
        var type__: Any? = null
        var is_var__: Boolean = false
        var delegate__: Any? = null
        var getter__: Any? = null
        var setter__: Any? = null
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

    open fun readIrProperty(): Any {
        var base__: Any? = null
        var name__: Any? = null
        var visibility__: Any? = null
        var modality__: Any? = null
        var is_var__: Boolean = false
        var is_const__: Boolean = false
        var is_lateinit__: Boolean = false
        var is_delegated__: Boolean = false
        var is_external__: Boolean = false
        var backing_field__: Any? = null
        var getter__: Any? = null
        var setter__: Any? = null
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

    open fun readIrVariable(): Any {
        var base__: Any? = null
        var name__: Any? = null
        var type__: Any? = null
        var is_var__: Boolean = false
        var is_const__: Boolean = false
        var is_lateinit__: Boolean = false
        var initializer__: Any? = null
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

    open fun readIrValueParameter(): Any {
        var base__: Any? = null
        var name__: Any? = null
        var index__: Int = 0
        var type__: Any? = null
        var vararg_element_type__: Any? = null
        var is_crossinline__: Boolean = false
        var is_noinline__: Boolean = false
        var default_value__: Any? = null
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

    open fun readIrTypeParameter(): Any {
        var base__: Any? = null
        var name__: Any? = null
        var index__: Int = 0
        var variance__: Any? = null
        var super_type__: MutableList<Any> = mutableListOf()
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

    open fun readIrTypeParameterContainer(): Any {
        var type_parameter__: MutableList<Any> = mutableListOf()
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

    open fun readIrClass(): Any {
        var base__: Any? = null
        var name__: Any? = null
        var kind__: Any? = null
        var visibility__: Any? = null
        var modality__: Any? = null
        var is_companion__: Boolean = false
        var is_inner__: Boolean = false
        var is_data__: Boolean = false
        var is_external__: Boolean = false
        var is_inline__: Boolean = false
        var this_receiver__: Any? = null
        var type_parameters__: Any? = null
        var declaration_container__: Any? = null
        var super_type__: MutableList<Any> = mutableListOf()
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

    open fun readIrTypeAlias(): Any {
        var base__: Any? = null
        var name__: Any? = null
        var visibility__: Any? = null
        var type_parameters__: Any? = null
        var expanded_type__: Any? = null
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

    open fun readIrEnumEntry(): Any {
        var base__: Any? = null
        var initializer__: Any? = null
        var corresponding_class__: Any? = null
        var name__: Any? = null
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

    open fun readIrAnonymousInit(): Any {
        var base__: Any? = null
        var body__: Any? = null
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

    open fun readIrDeclaration(): Any {
        var ir_anonymous_init__: Any? = null
        var ir_class__: Any? = null
        var ir_constructor__: Any? = null
        var ir_enum_entry__: Any? = null
        var ir_field__: Any? = null
        var ir_function__: Any? = null
        var ir_property__: Any? = null
        var ir_type_parameter__: Any? = null
        var ir_variable__: Any? = null
        var ir_value_parameter__: Any? = null
        var ir_local_delegated_property__: Any? = null
        var ir_type_alias__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> ir_anonymous_init__ = readWithLength { readIrAnonymousInit() }
                    2 -> ir_class__ = readWithLength { readIrClass() }
                    3 -> ir_constructor__ = readWithLength { readIrConstructor() }
                    4 -> ir_enum_entry__ = readWithLength { readIrEnumEntry() }
                    5 -> ir_field__ = readWithLength { readIrField() }
                    6 -> ir_function__ = readWithLength { readIrFunction() }
                    7 -> ir_property__ = readWithLength { readIrProperty() }
                    8 -> ir_type_parameter__ = readWithLength { readIrTypeParameter() }
                    9 -> ir_variable__ = readWithLength { readIrVariable() }
                    10 -> ir_value_parameter__ = readWithLength { readIrValueParameter() }
                    11 -> ir_local_delegated_property__ = readWithLength { readIrLocalDelegatedProperty() }
                    12 -> ir_type_alias__ = readWithLength { readIrTypeAlias() }
                    else -> skip(type)
                }
            }
        }
        return createIrDeclaration(ir_anonymous_init__, ir_class__, ir_constructor__, ir_enum_entry__, ir_field__, ir_function__, ir_property__, ir_type_parameter__, ir_variable__, ir_value_parameter__, ir_local_delegated_property__, ir_type_alias__)
    }

    open fun readIrBranch(): Any {
        var condition__: Any? = null
        var result__: Any? = null
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

    open fun readIrBlockBody(): Any {
        var statement__: MutableList<Any> = mutableListOf()
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

    open fun readIrCatch(): Any {
        var catch_parameter__: Any? = null
        var result__: Any? = null
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

    open fun readIrSyntheticBody(): Any {
        var kind__: Any? = null
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

    open fun readIrStatement(): Any {
        var coordinates__: Any? = null
        var declaration__: Any? = null
        var expression__: Any? = null
        var block_body__: Any? = null
        var branch__: Any? = null
        var catch__: Any? = null
        var synthetic_body__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> coordinates__ = readWithLength { readCoordinates() }
                    2 -> declaration__ = readWithLength { readIrDeclaration() }
                    3 -> expression__ = readWithLength { readIrExpression() }
                    4 -> block_body__ = readWithLength { readIrBlockBody() }
                    5 -> branch__ = readWithLength { readIrBranch() }
                    6 -> catch__ = readWithLength { readIrCatch() }
                    7 -> synthetic_body__ = readWithLength { readIrSyntheticBody() }
                    else -> skip(type)
                }
            }
        }
        return createIrStatement(coordinates__!!, declaration__, expression__, block_body__, branch__, catch__, synthetic_body__)
    }

}
