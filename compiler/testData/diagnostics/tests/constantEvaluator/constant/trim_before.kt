// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -IntrinsicConstEvaluation
// WITH_STDLIB

const val trim1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"123".trim()<!>
const val trim2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>""" 123
    456
""".trim()<!>


const val trimStart1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"123".trimStart()<!>
const val trimStart2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>""" 123 456 """.trimStart()<!>

const val trimEnd1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"123".trimEnd()<!>
const val trimEnd2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>""" 123 456 """.trimEnd()<!>

const val trimMargin1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"123".trimMargin()<!>
const val trimMargin2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"""ABC
                |123
                |456""".trimMargin()<!>
const val trimMargin3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"""
    #XYZ
    #foo
    #bar
""".trimMargin("#")<!>

const val trimMargin4 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, TRIM_MARGIN_BLANK_PREFIX!>"""
    #XYZ
    #foo
    #bar
""".trimMargin(" ")<!>


const val trimIndent1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"123".trimIndent()<!>
const val trimIndent2 =
    <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"""
            ABC
            123
            456
        """.trimIndent()<!>

/* GENERATED_FIR_TAGS: additiveExpression, const, equalityExpression, integerLiteral, propertyDeclaration,
unsignedLiteral */
