// FIR_IDENTICAL
// LANGUAGE: +ProperCheckAnnotationsTargetInTypeUsePositions
// ISSUE: KT-8325

fun foo() {
    object : @<!UNRESOLVED_REFERENCE!>__UNRESOLVED__<!> Any() {}
}
