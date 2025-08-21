/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils.serialization

// TODO: is it OK to just use UTF_8?
val SerializationCharset = Charsets.UTF_8

object StatementIds {
    const val RETURN = 0
    const val THROW = 1
    const val BREAK = 2
    const val CONTINUE = 3
    const val DEBUGGER = 4
    const val EXPRESSION = 5
    const val VARS = 6
    const val BLOCK = 7
    const val COMPOSITE_BLOCK = 8
    const val LABEL = 9
    const val IF = 10
    const val SWITCH = 11
    const val WHILE = 12
    const val DO_WHILE = 13
    const val FOR = 14
    const val FOR_IN = 15
    const val TRY = 16
    const val EMPTY = 17
    const val SINGLE_LINE_COMMENT = 18
    const val MULTI_LINE_COMMENT = 19
    const val IMPORT = 20
    const val EXPORT = 21
}

object ImportType {
    const val ALL = 0
    const val ITEMS = 1
    const val DEFAULT = 2
    const val EFFECT = 3
}

object ExportType {
    const val ALL = 0
    const val ITEMS = 1
}

object ExpressionIds {
    const val THIS_REF = 0
    const val NULL = 1
    const val TRUE_LITERAL = 2
    const val FALSE_LITERAL = 3
    const val STRING_LITERAL = 4
    const val REG_EXP = 5
    const val INT_LITERAL = 6
    const val DOUBLE_LITERAL = 7
    const val ARRAY_LITERAL = 8
    const val OBJECT_LITERAL = 9
    const val FUNCTION = 10
    const val DOC_COMMENT = 11
    const val BINARY_OPERATION = 12
    const val PREFIX_OPERATION = 13
    const val POSTFIX_OPERATION = 14
    const val CONDITIONAL = 15
    const val ARRAY_ACCESS = 16
    const val NAME_REFERENCE = 17
    const val SIMPLE_NAME_REFERENCE = 18
    const val PROPERTY_REFERENCE = 19
    const val INVOCATION = 20
    const val NEW = 21
    const val CLASS = 22
    const val SUPER_REF = 23
    const val YIELD = 24
    const val BIGINT_LITERAL = 25
}
