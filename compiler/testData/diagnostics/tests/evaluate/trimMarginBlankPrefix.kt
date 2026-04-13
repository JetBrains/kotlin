// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE
// WITH_STDLIB

const val blank = " "

val s1 = <!TRIM_MARGIN_BLANK_PREFIX!>"".trimMargin(" ")<!>
val s2 = "".trimMargin(blank)
val s3 = <!TRIM_MARGIN_BLANK_PREFIX!>"".trimMargin(
    """

    """)<!>

@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TRIM_MARGIN_BLANK_PREFIX!>"".trimMargin(" ")<!>) val a1 = 1
@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"".trimMargin(blank)<!>) val a2 = 1
@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TRIM_MARGIN_BLANK_PREFIX!>"".trimMargin("""

    """)<!>) val a3 = 1

annotation class Ann(val i : String)

/* GENERATED_FIR_TAGS: const, propertyDeclaration, stringLiteral */
