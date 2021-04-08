// !LANGUAGE: +ProperCheckAnnotationsTargetInTypeUsePositions
// ISSUE: KT-8325

fun foo() {
    object : @<!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>__UNRESOLVED__<!> Any() {}
}
