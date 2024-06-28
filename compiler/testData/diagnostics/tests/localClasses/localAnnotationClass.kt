// LANGUAGE: -ProhibitLocalAnnotations

fun f() {
    <!LOCAL_ANNOTATION_CLASS_WARNING!>annotation class Anno<!>

    @Anno class Local {
        <!LOCAL_ANNOTATION_CLASS_WARNING!>annotation <!NESTED_CLASS_NOT_ALLOWED!>class Nested<!><!>
    }
}
