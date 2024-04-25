// FIR_IDENTICAL
// LANGUAGE: +ProhibitLocalAnnotations

fun f() {
    <!LOCAL_ANNOTATION_CLASS_ERROR!>annotation class Anno<!>

    @Anno class Local {
        <!LOCAL_ANNOTATION_CLASS_ERROR!>annotation <!NESTED_CLASS_NOT_ALLOWED!>class Nested<!><!>
    }
}
