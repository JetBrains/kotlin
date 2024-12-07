// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-63063

class Test {
    @<!UNRESOLVED_REFERENCE!>ClassObjectAnnotation<!>
    @NestedAnnotation
    companion object {
        annotation class ClassObjectAnnotation
    }

    annotation class NestedAnnotation
}
