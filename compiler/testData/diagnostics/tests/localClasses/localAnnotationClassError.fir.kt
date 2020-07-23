// !LANGUAGE: +ProhibitLocalAnnotations

fun f() {
    <!LOCAL_ANNOTATION_CLASS_ERROR!>annotation class Anno<!>

    <!UNRESOLVED_REFERENCE!>@Anno<!> class Local {
        <!LOCAL_ANNOTATION_CLASS_ERROR!>annotation class Nested<!>
    }
}
