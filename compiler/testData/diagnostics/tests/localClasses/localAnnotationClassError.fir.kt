// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProhibitLocalAnnotations

fun f() {
    annotation <!LOCAL_ANNOTATION_CLASS_ERROR!>class Anno<!>

    @Anno class Local {
        annotation <!LOCAL_ANNOTATION_CLASS_ERROR, NESTED_CLASS_NOT_ALLOWED!>class Nested<!>
    }
}
