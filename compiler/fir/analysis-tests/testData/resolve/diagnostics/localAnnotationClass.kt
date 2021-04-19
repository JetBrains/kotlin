fun foo() {
    <!LOCAL_ANNOTATION_CLASS_ERROR!>annotation class Ann<!>

    @Ann class Local {
        // There should also be NESTED_CLASS_NOT_ALLOWED report here.
        <!LOCAL_ANNOTATION_CLASS_ERROR!>annotation <!NESTED_CLASS_NOT_ALLOWED!>class Nested<!><!>
    }
}
