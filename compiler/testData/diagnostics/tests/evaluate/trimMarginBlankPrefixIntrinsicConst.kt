// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE
// WITH_STDLIB
// LANGUAGE: +IntrinsicConstEvaluation

const val blank = " "
const val notBlank = "|"

const val c1 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, TRIM_MARGIN_BLANK_PREFIX!>"".trimMargin(" ")<!>
const val c2 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>"".trimMargin(blank)<!>
const val c3 = <!CONST_VAL_WITH_NON_CONST_INITIALIZER, TRIM_MARGIN_BLANK_PREFIX!>"".trimMargin(
    """

    """)<!>

const val c1b = "".trimMargin("|")
const val c2b = "".trimMargin(notBlank)

val s1 = <!TRIM_MARGIN_BLANK_PREFIX!>"".trimMargin(" ")<!>
val s2 = "".trimMargin(blank)
val s3 = <!TRIM_MARGIN_BLANK_PREFIX!>"".trimMargin(
    """

    """)<!>

val s1b = "".trimMargin("|")
val s2b = "".trimMargin(notBlank)

@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TRIM_MARGIN_BLANK_PREFIX!>"".trimMargin(" ")<!>) val a1 = 1
@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"".trimMargin(blank)<!>) val a2 = 1
@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TRIM_MARGIN_BLANK_PREFIX!>"".trimMargin("""

    """)<!>) val a3 = 1

@Ann("".trimMargin("|")) val a1b = 1
@Ann("".trimMargin(notBlank)) val a2b = 1

annotation class Ann(val i : String)

/* GENERATED_FIR_TAGS: const, propertyDeclaration, stringLiteral */
