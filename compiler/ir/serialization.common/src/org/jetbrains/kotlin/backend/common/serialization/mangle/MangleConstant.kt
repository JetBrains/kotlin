/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.mangle

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
        const val EXPECT_MARK = "#expect"
        const val UNKNOWN_MARK = "<unknown>"
        const val DYNAMIC_MARK = "<dynamic>"
        const val ERROR_MARK = "<ERROR CLASS>"
        const val ERROR_DECLARATION = "<ERROR DECLARATION>"
        const val STATIC_MEMBER_MARK = "#static"

        const val VARIANCE_SEPARATOR = '|'
        const val UPPER_BOUND_SEPARATOR = '§'
        const val PREFIX_SEPARATOR = ':'
        const val MODULE_SEPARATOR = '$'
        const val FQN_SEPARATOR = '.'
        const val INDEX_SEPARATOR = ':'

        const val PLATFORM_FUNCTION_MARKER = '%'

        const val EXTENSION_RECEIVER_PREFIX = '@'
        const val FUNCTION_NAME_PREFIX = '#'
        const val TYPE_PARAM_INDEX_PREFIX = '@'

        const val JAVA_FIELD_SUFFIX = "#jf"

        const val EMPTY_PREFIX = ""

        const val FUN_PREFIX = "kfun"
        const val CLASS_PREFIX = "kclass"
        const val PROPERTY_PREFIX = "kprop"
        const val FIELD_PREFIX = "kfield"
        const val ENUM_ENTRY_PREFIX = "kenumentry"
        const val TYPE_ALIAS_PREFIX = "ktypealias"
        const val TYPE_PARAM_PREFIX = "ktypeparam"
    }
}