/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.js.backend.ast.JsNameRef

object Namer {
    val KOTLIN_NAME = KotlinLanguage.NAME
    val KOTLIN_LOWER_NAME = KOTLIN_NAME.toLowerCase()

//    val EQUALS_METHOD_NAME = getStableMangledNameForDescriptor(JsPlatform.INSTANCE.getBuiltIns().getAny(), "equals")
//    val COMPARE_TO_METHOD_NAME = getStableMangledNameForDescriptor(JsPlatform.INSTANCE.getBuiltIns().getComparable(), "compareTo")
    val LONG_FROM_NUMBER = "fromNumber"
    val LONG_TO_NUMBER = "toNumber"
    val LONG_FROM_INT = "fromInt"
    val LONG_ZERO = "ZERO"
    val LONG_ONE = "ONE"
    val LONG_NEG_ONE = "NEG_ONE"
    val LONG_MAX_VALUE = "MAX_VALUE"
    val LONG_MIN_VALUE = "MIN_VALUE"
    val PRIMITIVE_COMPARE_TO = "primitiveCompareTo"
    val IS_CHAR = "isChar"
    val IS_NUMBER = "isNumber"
    val IS_CHAR_SEQUENCE = "isCharSequence"
    val GET_KCLASS = "getKClass"
    val GET_KCLASS_FROM_EXPRESSION = "getKClassFromExpression"

    val CALLEE_NAME = "\$fun"

    val CALL_FUNCTION = "call"
    val APPLY_FUNCTION = "apply"
    val BIND_FUNCTION = "bind"

    val SLICE_FUNCTION = "slice"
    val CONCAT_FUNCTION = "concat"

    val OUTER_NAME = "\$outer"
    val UNREACHABLE_NAME = "\$unreachable"

    val DELEGATE = "\$delegate"

    val ROOT_PACKAGE = "_"

    val EXTENSION_RECEIVER_NAME = "\$receiver"
    val IMPLICIT_RECEIVER_NAME = "this"
    val ANOTHER_THIS_PARAMETER_NAME = "\$this"

    val THROW_CLASS_CAST_EXCEPTION_FUN_NAME = "throwCCE"
    val THROW_ILLEGAL_STATE_EXCEPTION_FUN_NAME = "throwISE"
    val THROW_UNINITIALIZED_PROPERTY_ACCESS_EXCEPTION = "throwUPAE"
    val NULL_CHECK_INTRINSIC_NAME = "ensureNotNull"
    val PROTOTYPE_NAME = "prototype"
    val CONSTRUCTOR_NAME = "constructor"
    val CAPTURED_VAR_FIELD = "v"

    val IS_ARRAY_FUN_REF = JsNameRef("isArray", "Array")
    val DEFINE_INLINE_FUNCTION = "defineInlineFunction"
    val DEFAULT_PARAMETER_IMPLEMENTOR_SUFFIX = "\$default"

    val CONTINUATION = "\$cont"

    val JS_ERROR = JsNameRef("Error")

    val JS_OBJECT = JsNameRef("Object")
    val JS_OBJECT_CREATE_FUNCTION = JsNameRef("create", JS_OBJECT)

    val LOCAL_MODULE_PREFIX = "\$module\$"
    val METADATA = "\$metadata\$"
    val METADATA_INTERFACES = "interfaces"
    val METADATA_SIMPLE_NAME = "simpleName"
    val METADATA_CLASS_KIND = "kind"
    val CLASS_KIND_ENUM = "Kind"
    val CLASS_KIND_CLASS = "CLASS"
    val CLASS_KIND_INTERFACE = "INTERFACE"
    val CLASS_KIND_OBJECT = "OBJECT"

    val OBJECT_INSTANCE_VAR_SUFFIX = "_instance"
    val OBJECT_INSTANCE_FUNCTION_SUFFIX = "_getInstance"

    val ENUM_NAME_FIELD = "name\$"
    val ENUM_ORDINAL_FIELD = "ordinal\$"

    val IMPORTS_FOR_INLINE_PROPERTY = "\$\$importsForInline\$\$"

    val GETTER_PREFIX = "get_"
    val SETTER_PREFIX = "set_"

    val KCALLABLE_GET_NAME = "<get-name>"
    val KCALLABLE_NAME = "callableName"
    val KPROPERTY_GET = "get"
    val KPROPERTY_SET = "set"
    val KCALLABLE_CACHE_SUFFIX = "\$cache"

    val SETTER_ARGUMENT = "\$setValue"

    val THIS_SPECIAL_NAME = "<this>"
    val SET_SPECIAL_NAME = "<set-?>"
}