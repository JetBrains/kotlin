// RUN_PIPELINE_TILL: FRONTEND
fun foo() {
    annotation <!LOCAL_ANNOTATION_CLASS_ERROR!>class Ann<!>

    @Ann class Local {
        // There should also be NESTED_CLASS_NOT_ALLOWED report here.
        annotation <!LOCAL_ANNOTATION_CLASS_ERROR, NESTED_CLASS_NOT_ALLOWED!>class Nested<!>
    }
}
