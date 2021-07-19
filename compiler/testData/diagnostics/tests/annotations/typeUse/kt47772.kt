// FIR_IDENTICAL
// LANGUAGE: +ProperCheckAnnotationsTargetInTypeUsePositions
// ISSUE: KT-47772

@Target(AnnotationTarget.CLASS)
annotation class Bad

@Target(AnnotationTarget.TYPE)
annotation class Good

fun <K> materialize(): K? = null

fun test_error() {
    materialize<<!WRONG_ANNOTATION_TARGET!>@Bad<!> String>()
}

fun test_ok() {
    materialize<@Good String>()
}
