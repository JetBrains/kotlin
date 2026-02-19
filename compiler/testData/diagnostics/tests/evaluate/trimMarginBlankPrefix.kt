// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE
// WITH_STDLIB

const val blank = " "

val s1 = "".trimMargin(" ")
val s2 = "".trimMargin(blank)
val s3 = "".trimMargin(
    """

    """)

@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"".trimMargin(" ")<!>) val a1 = 1
@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"".trimMargin(blank)<!>) val a2 = 1
@Ann(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!>"".trimMargin("""

    """)<!>) val a3 = 1

annotation class Ann(val i : String)

/* GENERATED_FIR_TAGS: const, propertyDeclaration, stringLiteral */
