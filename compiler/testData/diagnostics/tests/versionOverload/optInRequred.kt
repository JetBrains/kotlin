// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

class C {
    fun foo(
        a: Int = 1,
        @<!OPT_IN_USAGE_ERROR!>IntroducedAt<!>("1") b: String = "hello",
        @<!OPT_IN_USAGE_ERROR!>IntroducedAt<!>("2") c: Boolean = true,
    ) = "$a/$b/$c"<!SYNTAX!><!>

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, stringLiteral */
