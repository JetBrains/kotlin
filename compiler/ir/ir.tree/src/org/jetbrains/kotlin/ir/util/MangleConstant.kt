/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

enum class MangleConstant(val prefix: Char, val separator: Char, val suffix: Char) {
    VALUE_PARAMETERS('(', ';', ')'),
    TYPE_PARAMETERS('{', ';', '}'),
    UPPER_BOUNDS('<', '&', '>'),
    TYPE_ARGUMENTS('<', ',', '>'),
    FLEXIBLE_TYPE('[', '~', ']');

    companion object {
        const val VAR_ARG_MARK = "..."
        const val STAR_MARK = '*'
        const val Q_MARK = '?'
        const val ENHANCED_NULLABILITY_MARK = "{EnhancedNullability}"
        const val DYNAMIC_MARK = "<dynamic>"
        const val ERROR_MARK = "<ERROR CLASS>"
        const val ERROR_DECLARATION = "<ERROR DECLARATION>"
        const val STATIC_MEMBER_MARK = "#static"
        const val TYPE_PARAMETER_MARKER_NAME = "<TP>"
        const val TYPE_PARAMETER_MARKER_NAME_SETTER = "<STP>"
        const val BACKING_FIELD_NAME = "<BF>"
        const val ANON_INIT_NAME_PREFIX = "<ANI>"
        const val ENUM_ENTRY_CLASS_NAME = "<EEC>"

        const val VARIANCE_SEPARATOR = '|'
        const val UPPER_BOUND_SEPARATOR = '§'
        const val FQN_SEPARATOR = '.'
        const val INDEX_SEPARATOR = ':'

        const val PLATFORM_FUNCTION_MARKER = '%'

        const val CONTEXT_RECEIVER_PREFIX = '!'
        const val EXTENSION_RECEIVER_PREFIX = '@'
        const val FUNCTION_NAME_PREFIX = '#'
        const val TYPE_PARAM_INDEX_PREFIX = '@'

        const val LOCAL_DECLARATION_INDEX_PREFIX = '$'

        const val JAVA_FIELD_SUFFIX = "#jf"

        const val FUN_PREFIX = "kfun"
        const val CLASS_PREFIX = "kclass"
        const val FIELD_PREFIX = "kfield"

        const val FIELD_MARK = '§'
        const val CONSTRUCTOR_MARK = "^c"
        const val GETTER_MARK = "^g"
        const val SETTER_MARK = "^s"
        const val DYNAMIC_TYPE_MARK = "^d"
        const val ERROR_TYPE_MARK = "^e"
        const val SUSPEND_MARK = 'S'
        const val EXTENSION_RECEIVER_MARK = '@'
        const val LOCAL_CLASS_MARK = '$'
        const val RETURN_TYPE_MARK = '='
        const val NAME_SEPARATOR = '/'
    }
}

fun <T> Collection<T>.collectForMangler(builder: StringBuilder, params: MangleConstant, collect: StringBuilder.(T) -> Unit) {
    var first = true

    builder.append(params.prefix)

    var addSeparator = true

    for (e in this) {
        if (first) {
            first = false
        } else if (addSeparator) {
            builder.append(params.separator)
        }

        val l = builder.length
        builder.collect(e)
        addSeparator = l < builder.length
    }

    if (!addSeparator) {
        if (builder.last() == params.separator) {
            // avoid signatures like foo(Int;)
            builder.deleteCharAt(builder.lastIndex)
        }
    }

    builder.append(params.suffix)
}

