// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// FIR_IDENTICAL

<!IGNORABILITY_ANNOTATIONS_WITH_CHECKER_DISABLED!>@file:MustUseReturnValue<!>

fun foo(): String = ""

<!IGNORABILITY_ANNOTATIONS_WITH_CHECKER_DISABLED!>@IgnorableReturnValue<!>
fun bar(): Int = 42

<!IGNORABILITY_ANNOTATIONS_WITH_CHECKER_DISABLED!>@MustUseReturnValue<!>
class Test {
    <!IGNORABILITY_ANNOTATIONS_WITH_CHECKER_DISABLED!>@IgnorableReturnValue<!>
    fun method(): Double = 0.0
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classDeclaration, functionDeclaration, integerLiteral, stringLiteral */
