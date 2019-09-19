/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.nextgen

class SimpleIrProtoReader(source: ByteArray) : ProtoReader(source) {
    fun readDescriptorReference(): Any {
        var package_fq_name__: Any? = null
        var class_fq_name__: Any? = null
        var name__: Any? = null
        var uniq_id__: Any? = null
        var is_getter__: Any? = null
        var is_setter__: Any? = null
        var is_backing_field__: Any? = null
        var is_fake_override__: Any? = null
        var is_default_constructor__: Any? = null
        var is_enum_entry__: Any? = null
        var is_enum_special__: Any? = null
        var is_type_parameter__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> package_fq_name__ = readFqName()
                    2 -> class_fq_name__ = readFqName()
                    3 -> name__ = readIrDataIndex()
                    4 -> uniq_id__ = readUniqId()
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
        return arrayOf(package_fq_name__, class_fq_name__, name__, uniq_id__, is_getter__, is_setter__, is_backing_field__, is_fake_override__, is_default_constructor__, is_enum_entry__, is_enum_special__, is_type_parameter__)
    }

    fun readUniqId(): Any {
        var index__: Any? = null
        var isLocal__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> index__ = readInt64()
                    2 -> isLocal__ = readBool()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(index__, isLocal__)
    }

    fun readCoordinates(): Any {
        var start_offset__: Any? = null
        var end_offset__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> start_offset__ = readInt32()
                    2 -> end_offset__ = readInt32()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(start_offset__, end_offset__)
    }

    fun readVisibility(): Any {
        var name__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> name__ = readIrDataIndex()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(name__)
    }

    fun readIrStatementOrigin(): Any {
        var name__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> name__ = readIrDataIndex()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(name__)
    }

    fun readIrDeclarationOrigin(): Any {
        var origin__: Any? = null
        var custom__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> origin__ = readInt32()
                    2 -> custom__ = readIrDataIndex()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(origin__, custom__)
    }

    fun readIrDataIndex(): Any {
        var index__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> index__ = readInt32()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(index__)
    }

    fun readFqName(): Any {
        var segment__: List<Any> = mutableListOf<Any>()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> segment__ += readIrDataIndex()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(segment__)
    }

    fun readIrDeclarationContainer(): Any {
        var declaration__: List<Any> = mutableListOf<Any>()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> declaration__ += readIrDeclaration()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(declaration__)
    }

    fun readFileEntry(): Any {
        var name__: Any? = null
        var line_start_offsets__: List<Any> = mutableListOf<Any>()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> name__ = readString()
                    2 -> line_start_offsets__ += readInt32()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(name__, line_start_offsets__)
    }

    fun readIrFile(): Any {
        var declaration_id__: List<Any> = mutableListOf<Any>()
        var file_entry__: Any? = null
        var fq_name__: Any? = null
        var annotations__: Any? = null
        var explicitly_exported_to_compiler__: List<Any> = mutableListOf<Any>()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> declaration_id__ += readUniqId()
                    2 -> file_entry__ = readFileEntry()
                    3 -> fq_name__ = readFqName()
                    4 -> annotations__ = readAnnotations()
                    5 -> explicitly_exported_to_compiler__ += readIrDataIndex()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(declaration_id__, file_entry__, fq_name__, annotations__, explicitly_exported_to_compiler__)
    }

    fun readStringTable(): Any {
        var strings__: List<Any> = mutableListOf<Any>()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> strings__ += readString()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(strings__)
    }

    fun readIrSymbolData(): Any {
        var kind__: Any? = null
        var uniq_id__: Any? = null
        var top_level_uniq_id__: Any? = null
        var fqname__: Any? = null
        var descriptor_reference__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> kind__ = readInt32()
                    2 -> uniq_id__ = readUniqId()
                    3 -> top_level_uniq_id__ = readUniqId()
                    4 -> fqname__ = readFqName()
                    5 -> descriptor_reference__ = readDescriptorReference()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(kind__, uniq_id__, top_level_uniq_id__, fqname__, descriptor_reference__)
    }

    fun readIrSymbolTable(): Any {
        var symbols__: List<Any> = mutableListOf<Any>()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbols__ += readIrSymbolData()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(symbols__)
    }

    fun readAnnotations(): Any {
        var annotation__: List<Any> = mutableListOf<Any>()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> annotation__ += readIrConstructorCall()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(annotation__)
    }

    fun readTypeArguments(): Any {
        var type_argument__: List<Any> = mutableListOf<Any>()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> type_argument__ += readIrDataIndex()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(type_argument__)
    }

    fun readIrStarProjection(): Any {
        var void__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> void__ = readBool()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(void__)
    }

    fun readIrTypeProjection(): Any {
        var variance__: Any? = null
        var type__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> variance__ = readInt32()
                    2 -> type__ = readIrDataIndex()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(variance__, type__)
    }

    fun readIrTypeArgument(): Any {
        var star__: Any? = null
        var type__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> star__ = readIrStarProjection()
                    2 -> type__ = readIrTypeProjection()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(star__, type__)
    }

    fun readIrSimpleType(): Any {
        var annotations__: Any? = null
        var classifier__: Any? = null
        var has_question_mark__: Any? = null
        var argument__: List<Any> = mutableListOf<Any>()
        var abbreviation__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> annotations__ = readAnnotations()
                    2 -> classifier__ = readIrDataIndex()
                    3 -> has_question_mark__ = readBool()
                    4 -> argument__ += readIrTypeArgument()
                    5 -> abbreviation__ = readIrTypeAbbreviation()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(annotations__, classifier__, has_question_mark__, argument__, abbreviation__)
    }

    fun readIrTypeAbbreviation(): Any {
        var annotations__: Any? = null
        var type_alias__: Any? = null
        var has_question_mark__: Any? = null
        var argument__: List<Any> = mutableListOf<Any>()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> annotations__ = readAnnotations()
                    2 -> type_alias__ = readIrDataIndex()
                    3 -> has_question_mark__ = readBool()
                    4 -> argument__ += readIrTypeArgument()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(annotations__, type_alias__, has_question_mark__, argument__)
    }

    fun readIrDynamicType(): Any {
        var annotations__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> annotations__ = readAnnotations()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(annotations__)
    }

    fun readIrErrorType(): Any {
        var annotations__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> annotations__ = readAnnotations()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(annotations__)
    }

    fun readIrType(): Any {
        var simple__: Any? = null
        var dynamic__: Any? = null
        var error__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> simple__ = readIrSimpleType()
                    2 -> dynamic__ = readIrDynamicType()
                    3 -> error__ = readIrErrorType()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(simple__, dynamic__, error__)
    }

    fun readIrTypeTable(): Any {
        var types__: List<Any> = mutableListOf<Any>()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> types__ += readIrType()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(types__)
    }

    fun readIrBreak(): Any {
        var loop_id__: Any? = null
        var label__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> loop_id__ = readInt32()
                    2 -> label__ = readIrDataIndex()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(loop_id__, label__)
    }

    fun readIrBlock(): Any {
        var origin__: Any? = null
        var statement__: List<Any> = mutableListOf<Any>()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> origin__ = readIrStatementOrigin()
                    2 -> statement__ += readIrStatement()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(origin__, statement__)
    }

    fun readMemberAccessCommon(): Any {
        var dispatch_receiver__: Any? = null
        var extension_receiver__: Any? = null
        var value_argument__: List<Any> = mutableListOf<Any>()
        var type_arguments__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> dispatch_receiver__ = readIrExpression()
                    2 -> extension_receiver__ = readIrExpression()
                    3 -> value_argument__ += readNullableIrExpression()
                    4 -> type_arguments__ = readTypeArguments()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(dispatch_receiver__, extension_receiver__, value_argument__, type_arguments__)
    }

    fun readIrCall(): Any {
        var symbol__: Any? = null
        var member_access__: Any? = null
        var super__: Any? = null
        var origin__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol__ = readIrDataIndex()
                    2 -> member_access__ = readMemberAccessCommon()
                    3 -> super__ = readIrDataIndex()
                    4 -> origin__ = readIrStatementOrigin()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(symbol__, member_access__, super__, origin__)
    }

    fun readIrConstructorCall(): Any {
        var symbol__: Any? = null
        var constructor_type_arguments_count__: Any? = null
        var member_access__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol__ = readIrDataIndex()
                    2 -> constructor_type_arguments_count__ = readInt32()
                    3 -> member_access__ = readMemberAccessCommon()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(symbol__, constructor_type_arguments_count__, member_access__)
    }

    fun readIrFunctionReference(): Any {
        var symbol__: Any? = null
        var origin__: Any? = null
        var member_access__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol__ = readIrDataIndex()
                    2 -> origin__ = readIrStatementOrigin()
                    3 -> member_access__ = readMemberAccessCommon()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(symbol__, origin__, member_access__)
    }

    fun readIrLocalDelegatedPropertyReference(): Any {
        var delegate__: Any? = null
        var getter__: Any? = null
        var setter__: Any? = null
        var symbol__: Any? = null
        var origin__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> delegate__ = readIrDataIndex()
                    2 -> getter__ = readIrDataIndex()
                    3 -> setter__ = readIrDataIndex()
                    4 -> symbol__ = readIrDataIndex()
                    5 -> origin__ = readIrStatementOrigin()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(delegate__, getter__, setter__, symbol__, origin__)
    }

    fun readIrPropertyReference(): Any {
        var field__: Any? = null
        var getter__: Any? = null
        var setter__: Any? = null
        var origin__: Any? = null
        var member_access__: Any? = null
        var symbol__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> field__ = readIrDataIndex()
                    2 -> getter__ = readIrDataIndex()
                    3 -> setter__ = readIrDataIndex()
                    4 -> origin__ = readIrStatementOrigin()
                    5 -> member_access__ = readMemberAccessCommon()
                    6 -> symbol__ = readIrDataIndex()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(field__, getter__, setter__, origin__, member_access__, symbol__)
    }

    fun readIrComposite(): Any {
        var statement__: List<Any> = mutableListOf<Any>()
        var origin__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> statement__ += readIrStatement()
                    2 -> origin__ = readIrStatementOrigin()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(statement__, origin__)
    }

    fun readIrClassReference(): Any {
        var class_symbol__: Any? = null
        var class_type__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> class_symbol__ = readIrDataIndex()
                    2 -> class_type__ = readIrDataIndex()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(class_symbol__, class_type__)
    }

    fun readIrConst(): Any {
        var null__: Any? = null
        var boolean__: Any? = null
        var char__: Any? = null
        var byte__: Any? = null
        var short__: Any? = null
        var int__: Any? = null
        var long__: Any? = null
        var float__: Any? = null
        var double__: Any? = null
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
                    10 -> string__ = readIrDataIndex()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(null__, boolean__, char__, byte__, short__, int__, long__, float__, double__, string__)
    }

    fun readIrContinue(): Any {
        var loop_id__: Any? = null
        var label__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> loop_id__ = readInt32()
                    2 -> label__ = readIrDataIndex()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(loop_id__, label__)
    }

    fun readIrDelegatingConstructorCall(): Any {
        var symbol__: Any? = null
        var member_access__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol__ = readIrDataIndex()
                    2 -> member_access__ = readMemberAccessCommon()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(symbol__, member_access__)
    }

    fun readIrDoWhile(): Any {
        var loop__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> loop__ = readLoop()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(loop__)
    }

    fun readIrEnumConstructorCall(): Any {
        var symbol__: Any? = null
        var member_access__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol__ = readIrDataIndex()
                    2 -> member_access__ = readMemberAccessCommon()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(symbol__, member_access__)
    }

    fun readIrGetClass(): Any {
        var argument__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> argument__ = readIrExpression()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(argument__)
    }

    fun readIrGetEnumValue(): Any {
        var symbol__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    2 -> symbol__ = readIrDataIndex()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(symbol__)
    }

    fun readFieldAccessCommon(): Any {
        var symbol__: Any? = null
        var super__: Any? = null
        var receiver__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol__ = readIrDataIndex()
                    2 -> super__ = readIrDataIndex()
                    3 -> receiver__ = readIrExpression()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(symbol__, super__, receiver__)
    }

    fun readIrGetField(): Any {
        var field_access__: Any? = null
        var origin__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> field_access__ = readFieldAccessCommon()
                    2 -> origin__ = readIrStatementOrigin()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(field_access__, origin__)
    }

    fun readIrGetValue(): Any {
        var symbol__: Any? = null
        var origin__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol__ = readIrDataIndex()
                    2 -> origin__ = readIrStatementOrigin()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(symbol__, origin__)
    }

    fun readIrGetObject(): Any {
        var symbol__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol__ = readIrDataIndex()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(symbol__)
    }

    fun readIrInstanceInitializerCall(): Any {
        var symbol__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol__ = readIrDataIndex()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(symbol__)
    }

    fun readLoop(): Any {
        var loop_id__: Any? = null
        var condition__: Any? = null
        var label__: Any? = null
        var body__: Any? = null
        var origin__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> loop_id__ = readInt32()
                    2 -> condition__ = readIrExpression()
                    3 -> label__ = readIrDataIndex()
                    4 -> body__ = readIrExpression()
                    5 -> origin__ = readIrStatementOrigin()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(loop_id__, condition__, label__, body__, origin__)
    }

    fun readIrReturn(): Any {
        var return_target__: Any? = null
        var value__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> return_target__ = readIrDataIndex()
                    2 -> value__ = readIrExpression()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(return_target__, value__)
    }

    fun readIrSetField(): Any {
        var field_access__: Any? = null
        var value__: Any? = null
        var origin__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> field_access__ = readFieldAccessCommon()
                    2 -> value__ = readIrExpression()
                    3 -> origin__ = readIrStatementOrigin()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(field_access__, value__, origin__)
    }

    fun readIrSetVariable(): Any {
        var symbol__: Any? = null
        var value__: Any? = null
        var origin__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol__ = readIrDataIndex()
                    2 -> value__ = readIrExpression()
                    3 -> origin__ = readIrStatementOrigin()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(symbol__, value__, origin__)
    }

    fun readIrSpreadElement(): Any {
        var expression__: Any? = null
        var coordinates__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> expression__ = readIrExpression()
                    2 -> coordinates__ = readCoordinates()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(expression__, coordinates__)
    }

    fun readIrStringConcat(): Any {
        var argument__: List<Any> = mutableListOf<Any>()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> argument__ += readIrExpression()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(argument__)
    }

    fun readIrThrow(): Any {
        var value__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> value__ = readIrExpression()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(value__)
    }

    fun readIrTry(): Any {
        var result__: Any? = null
        var catch__: List<Any> = mutableListOf<Any>()
        var finally__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> result__ = readIrExpression()
                    2 -> catch__ += readIrStatement()
                    3 -> finally__ = readIrExpression()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(result__, catch__, finally__)
    }

    fun readIrTypeOp(): Any {
        var operator__: Any? = null
        var operand__: Any? = null
        var argument__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> operator__ = readInt32()
                    2 -> operand__ = readIrDataIndex()
                    3 -> argument__ = readIrExpression()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(operator__, operand__, argument__)
    }

    fun readIrVararg(): Any {
        var element_type__: Any? = null
        var element__: List<Any> = mutableListOf<Any>()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> element_type__ = readIrDataIndex()
                    2 -> element__ += readIrVarargElement()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(element_type__, element__)
    }

    fun readIrVarargElement(): Any {
        var expression__: Any? = null
        var spread_element__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> expression__ = readIrExpression()
                    2 -> spread_element__ = readIrSpreadElement()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(expression__, spread_element__)
    }

    fun readIrWhen(): Any {
        var branch__: List<Any> = mutableListOf<Any>()
        var origin__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> branch__ += readIrStatement()
                    2 -> origin__ = readIrStatementOrigin()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(branch__, origin__)
    }

    fun readIrWhile(): Any {
        var loop__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> loop__ = readLoop()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(loop__)
    }

    fun readIrFunctionExpression(): Any {
        var function__: Any? = null
        var origin__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> function__ = readIrFunction()
                    2 -> origin__ = readIrStatementOrigin()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(function__, origin__)
    }

    fun readIrDynamicMemberExpression(): Any {
        var memberName__: Any? = null
        var receiver__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> memberName__ = readIrDataIndex()
                    2 -> receiver__ = readIrExpression()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(memberName__, receiver__)
    }

    fun readIrDynamicOperatorExpression(): Any {
        var operator__: Any? = null
        var receiver__: Any? = null
        var argument__: List<Any> = mutableListOf<Any>()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> operator__ = readInt32()
                    2 -> receiver__ = readIrExpression()
                    3 -> argument__ += readIrExpression()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(operator__, receiver__, argument__)
    }

    fun readIrOperation(): Any {
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
                    1 -> block__ = readIrBlock()
                    2 -> break__ = readIrBreak()
                    3 -> call__ = readIrCall()
                    4 -> class_reference__ = readIrClassReference()
                    5 -> composite__ = readIrComposite()
                    6 -> const__ = readIrConst()
                    7 -> continue__ = readIrContinue()
                    8 -> delegating_constructor_call__ = readIrDelegatingConstructorCall()
                    9 -> do_while__ = readIrDoWhile()
                    10 -> enum_constructor_call__ = readIrEnumConstructorCall()
                    11 -> function_reference__ = readIrFunctionReference()
                    12 -> get_class__ = readIrGetClass()
                    13 -> get_enum_value__ = readIrGetEnumValue()
                    14 -> get_field__ = readIrGetField()
                    15 -> get_object__ = readIrGetObject()
                    16 -> get_value__ = readIrGetValue()
                    17 -> instance_initializer_call__ = readIrInstanceInitializerCall()
                    18 -> property_reference__ = readIrPropertyReference()
                    19 -> return__ = readIrReturn()
                    20 -> set_field__ = readIrSetField()
                    21 -> set_variable__ = readIrSetVariable()
                    22 -> string_concat__ = readIrStringConcat()
                    23 -> throw__ = readIrThrow()
                    24 -> try__ = readIrTry()
                    25 -> type_op__ = readIrTypeOp()
                    26 -> vararg__ = readIrVararg()
                    27 -> when__ = readIrWhen()
                    28 -> while__ = readIrWhile()
                    29 -> dynamic_member__ = readIrDynamicMemberExpression()
                    30 -> dynamic_operator__ = readIrDynamicOperatorExpression()
                    31 -> local_delegated_property_reference__ = readIrLocalDelegatedPropertyReference()
                    32 -> constructor_call__ = readIrConstructorCall()
                    33 -> function_expression__ = readIrFunctionExpression()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(block__, break__, call__, class_reference__, composite__, const__, continue__, delegating_constructor_call__, do_while__, enum_constructor_call__, function_reference__, get_class__, get_enum_value__, get_field__, get_object__, get_value__, instance_initializer_call__, property_reference__, return__, set_field__, set_variable__, string_concat__, throw__, try__, type_op__, vararg__, when__, while__, dynamic_member__, dynamic_operator__, local_delegated_property_reference__, constructor_call__, function_expression__)
    }

    fun readIrExpression(): Any {
        var operation__: Any? = null
        var type__: Any? = null
        var coordinates__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> operation__ = readIrOperation()
                    2 -> type__ = readIrDataIndex()
                    3 -> coordinates__ = readCoordinates()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(operation__, type__, coordinates__)
    }

    fun readNullableIrExpression(): Any {
        var expression__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> expression__ = readIrExpression()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(expression__)
    }

    fun readIrDeclarationBase(): Any {
        var symbol__: Any? = null
        var origin__: Any? = null
        var coordinates__: Any? = null
        var annotations__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> symbol__ = readIrDataIndex()
                    2 -> origin__ = readIrDeclarationOrigin()
                    3 -> coordinates__ = readCoordinates()
                    4 -> annotations__ = readAnnotations()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(symbol__, origin__, coordinates__, annotations__)
    }

    fun readIrFunctionBase(): Any {
        var base__: Any? = null
        var name__: Any? = null
        var visibility__: Any? = null
        var is_inline__: Any? = null
        var is_external__: Any? = null
        var type_parameters__: Any? = null
        var dispatch_receiver__: Any? = null
        var extension_receiver__: Any? = null
        var value_parameter__: List<Any> = mutableListOf<Any>()
        var body__: Any? = null
        var return_type__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> base__ = readIrDeclarationBase()
                    2 -> name__ = readIrDataIndex()
                    3 -> visibility__ = readVisibility()
                    4 -> is_inline__ = readBool()
                    5 -> is_external__ = readBool()
                    6 -> type_parameters__ = readIrTypeParameterContainer()
                    7 -> dispatch_receiver__ = readIrValueParameter()
                    8 -> extension_receiver__ = readIrValueParameter()
                    9 -> value_parameter__ += readIrValueParameter()
                    10 -> body__ = readIrDataIndex()
                    11 -> return_type__ = readIrDataIndex()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(base__, name__, visibility__, is_inline__, is_external__, type_parameters__, dispatch_receiver__, extension_receiver__, value_parameter__, body__, return_type__)
    }

    fun readIrFunction(): Any {
        var base__: Any? = null
        var modality__: Any? = null
        var is_tailrec__: Any? = null
        var is_suspend__: Any? = null
        var overridden__: List<Any> = mutableListOf<Any>()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> base__ = readIrFunctionBase()
                    2 -> modality__ = readInt32()
                    3 -> is_tailrec__ = readBool()
                    4 -> is_suspend__ = readBool()
                    5 -> overridden__ += readIrDataIndex()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(base__, modality__, is_tailrec__, is_suspend__, overridden__)
    }

    fun readIrConstructor(): Any {
        var base__: Any? = null
        var is_primary__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> base__ = readIrFunctionBase()
                    2 -> is_primary__ = readBool()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(base__, is_primary__)
    }

    fun readIrField(): Any {
        var base__: Any? = null
        var initializer__: Any? = null
        var name__: Any? = null
        var visibility__: Any? = null
        var is_final__: Any? = null
        var is_external__: Any? = null
        var is_static__: Any? = null
        var type__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> base__ = readIrDeclarationBase()
                    2 -> initializer__ = readIrDataIndex()
                    3 -> name__ = readIrDataIndex()
                    4 -> visibility__ = readVisibility()
                    5 -> is_final__ = readBool()
                    6 -> is_external__ = readBool()
                    7 -> is_static__ = readBool()
                    8 -> type__ = readIrDataIndex()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(base__, initializer__, name__, visibility__, is_final__, is_external__, is_static__, type__)
    }

    fun readIrLocalDelegatedProperty(): Any {
        var base__: Any? = null
        var name__: Any? = null
        var type__: Any? = null
        var is_var__: Any? = null
        var delegate__: Any? = null
        var getter__: Any? = null
        var setter__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> base__ = readIrDeclarationBase()
                    2 -> name__ = readIrDataIndex()
                    3 -> type__ = readIrDataIndex()
                    4 -> is_var__ = readBool()
                    5 -> delegate__ = readIrVariable()
                    6 -> getter__ = readIrFunction()
                    7 -> setter__ = readIrFunction()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(base__, name__, type__, is_var__, delegate__, getter__, setter__)
    }

    fun readIrProperty(): Any {
        var base__: Any? = null
        var name__: Any? = null
        var visibility__: Any? = null
        var modality__: Any? = null
        var is_var__: Any? = null
        var is_const__: Any? = null
        var is_lateinit__: Any? = null
        var is_delegated__: Any? = null
        var is_external__: Any? = null
        var backing_field__: Any? = null
        var getter__: Any? = null
        var setter__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> base__ = readIrDeclarationBase()
                    2 -> name__ = readIrDataIndex()
                    3 -> visibility__ = readVisibility()
                    4 -> modality__ = readInt32()
                    5 -> is_var__ = readBool()
                    6 -> is_const__ = readBool()
                    7 -> is_lateinit__ = readBool()
                    8 -> is_delegated__ = readBool()
                    9 -> is_external__ = readBool()
                    10 -> backing_field__ = readIrField()
                    11 -> getter__ = readIrFunction()
                    12 -> setter__ = readIrFunction()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(base__, name__, visibility__, modality__, is_var__, is_const__, is_lateinit__, is_delegated__, is_external__, backing_field__, getter__, setter__)
    }

    fun readIrVariable(): Any {
        var base__: Any? = null
        var name__: Any? = null
        var type__: Any? = null
        var is_var__: Any? = null
        var is_const__: Any? = null
        var is_lateinit__: Any? = null
        var initializer__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> base__ = readIrDeclarationBase()
                    2 -> name__ = readIrDataIndex()
                    3 -> type__ = readIrDataIndex()
                    4 -> is_var__ = readBool()
                    5 -> is_const__ = readBool()
                    6 -> is_lateinit__ = readBool()
                    7 -> initializer__ = readIrExpression()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(base__, name__, type__, is_var__, is_const__, is_lateinit__, initializer__)
    }

    fun readIrValueParameter(): Any {
        var base__: Any? = null
        var name__: Any? = null
        var index__: Any? = null
        var type__: Any? = null
        var vararg_element_type__: Any? = null
        var is_crossinline__: Any? = null
        var is_noinline__: Any? = null
        var default_value__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> base__ = readIrDeclarationBase()
                    2 -> name__ = readIrDataIndex()
                    3 -> index__ = readInt32()
                    4 -> type__ = readIrDataIndex()
                    5 -> vararg_element_type__ = readIrDataIndex()
                    6 -> is_crossinline__ = readBool()
                    7 -> is_noinline__ = readBool()
                    8 -> default_value__ = readIrDataIndex()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(base__, name__, index__, type__, vararg_element_type__, is_crossinline__, is_noinline__, default_value__)
    }

    fun readIrTypeParameter(): Any {
        var base__: Any? = null
        var name__: Any? = null
        var index__: Any? = null
        var variance__: Any? = null
        var super_type__: List<Any> = mutableListOf<Any>()
        var is_reified__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> base__ = readIrDeclarationBase()
                    2 -> name__ = readIrDataIndex()
                    3 -> index__ = readInt32()
                    4 -> variance__ = readInt32()
                    5 -> super_type__ += readIrDataIndex()
                    6 -> is_reified__ = readBool()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(base__, name__, index__, variance__, super_type__, is_reified__)
    }

    fun readIrTypeParameterContainer(): Any {
        var type_parameter__: List<Any> = mutableListOf<Any>()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> type_parameter__ += readIrTypeParameter()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(type_parameter__)
    }

    fun readIrClass(): Any {
        var base__: Any? = null
        var name__: Any? = null
        var kind__: Any? = null
        var visibility__: Any? = null
        var modality__: Any? = null
        var is_companion__: Any? = null
        var is_inner__: Any? = null
        var is_data__: Any? = null
        var is_external__: Any? = null
        var is_inline__: Any? = null
        var this_receiver__: Any? = null
        var type_parameters__: Any? = null
        var declaration_container__: Any? = null
        var super_type__: List<Any> = mutableListOf<Any>()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> base__ = readIrDeclarationBase()
                    2 -> name__ = readIrDataIndex()
                    3 -> kind__ = readInt32()
                    4 -> visibility__ = readVisibility()
                    5 -> modality__ = readInt32()
                    6 -> is_companion__ = readBool()
                    7 -> is_inner__ = readBool()
                    8 -> is_data__ = readBool()
                    9 -> is_external__ = readBool()
                    10 -> is_inline__ = readBool()
                    11 -> this_receiver__ = readIrValueParameter()
                    12 -> type_parameters__ = readIrTypeParameterContainer()
                    13 -> declaration_container__ = readIrDeclarationContainer()
                    14 -> super_type__ += readIrDataIndex()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(base__, name__, kind__, visibility__, modality__, is_companion__, is_inner__, is_data__, is_external__, is_inline__, this_receiver__, type_parameters__, declaration_container__, super_type__)
    }

    fun readIrTypeAlias(): Any {
        var base__: Any? = null
        var name__: Any? = null
        var visibility__: Any? = null
        var type_parameters__: Any? = null
        var expanded_type__: Any? = null
        var is_actual__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> base__ = readIrDeclarationBase()
                    2 -> name__ = readIrDataIndex()
                    3 -> visibility__ = readVisibility()
                    4 -> type_parameters__ = readIrTypeParameterContainer()
                    5 -> expanded_type__ = readIrDataIndex()
                    6 -> is_actual__ = readBool()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(base__, name__, visibility__, type_parameters__, expanded_type__, is_actual__)
    }

    fun readIrEnumEntry(): Any {
        var base__: Any? = null
        var initializer__: Any? = null
        var corresponding_class__: Any? = null
        var name__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> base__ = readIrDeclarationBase()
                    2 -> initializer__ = readIrDataIndex()
                    3 -> corresponding_class__ = readIrClass()
                    4 -> name__ = readIrDataIndex()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(base__, initializer__, corresponding_class__, name__)
    }

    fun readIrAnonymousInit(): Any {
        var base__: Any? = null
        var body__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> base__ = readIrDeclarationBase()
                    2 -> body__ = readIrDataIndex()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(base__, body__)
    }

    fun readIrDeclaration(): Any {
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
                    1 -> ir_anonymous_init__ = readIrAnonymousInit()
                    2 -> ir_class__ = readIrClass()
                    3 -> ir_constructor__ = readIrConstructor()
                    4 -> ir_enum_entry__ = readIrEnumEntry()
                    5 -> ir_field__ = readIrField()
                    6 -> ir_function__ = readIrFunction()
                    7 -> ir_property__ = readIrProperty()
                    8 -> ir_type_parameter__ = readIrTypeParameter()
                    9 -> ir_variable__ = readIrVariable()
                    10 -> ir_value_parameter__ = readIrValueParameter()
                    11 -> ir_local_delegated_property__ = readIrLocalDelegatedProperty()
                    12 -> ir_type_alias__ = readIrTypeAlias()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(ir_anonymous_init__, ir_class__, ir_constructor__, ir_enum_entry__, ir_field__, ir_function__, ir_property__, ir_type_parameter__, ir_variable__, ir_value_parameter__, ir_local_delegated_property__, ir_type_alias__)
    }

    fun readIrBranch(): Any {
        var condition__: Any? = null
        var result__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> condition__ = readIrExpression()
                    2 -> result__ = readIrExpression()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(condition__, result__)
    }

    fun readIrBlockBody(): Any {
        var statement__: List<Any> = mutableListOf<Any>()
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> statement__ += readIrStatement()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(statement__)
    }

    fun readIrCatch(): Any {
        var catch_parameter__: Any? = null
        var result__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> catch_parameter__ = readIrVariable()
                    2 -> result__ = readIrExpression()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(catch_parameter__, result__)
    }

    fun readIrSyntheticBody(): Any {
        var kind__: Any? = null
        while (hasData) {
            readField { fieldNumber, type ->
                when (fieldNumber) {
                    1 -> kind__ = readInt32()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(kind__)
    }

    fun readIrStatement(): Any {
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
                    1 -> coordinates__ = readCoordinates()
                    2 -> declaration__ = readIrDeclaration()
                    3 -> expression__ = readIrExpression()
                    4 -> block_body__ = readIrBlockBody()
                    5 -> branch__ = readIrBranch()
                    6 -> catch__ = readIrCatch()
                    7 -> synthetic_body__ = readIrSyntheticBody()
                    else -> skip(type)
                }
            }
        }
        return arrayOf(coordinates__, declaration__, expression__, block_body__, branch__, catch__, synthetic_body__)
    }

}
