// LANGUAGE: +ProperCheckAnnotationsTargetInTypeUsePositions
// DIAGNOSTICS: -USELESS_CAST
// ISSUE: KT-46173

@Target(AnnotationTarget.TYPE)
annotation class Ann(val s: String)

fun some(): Int {
    return 1 as @Ann(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>6<!>) Int // should error but doesn't
}
