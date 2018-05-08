// !LANGUAGE: -ProhibitLocalAnnotations

fun f() {
    <!LOCAL_ANNOTATION_CLASS!>annotation class Anno<!>

    @Anno class Local {
        <!LOCAL_ANNOTATION_CLASS!>annotation <!NESTED_CLASS_NOT_ALLOWED!>class Nested<!><!>
    }
}
