// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

class A {
    fun foo(a: String): String { return a }
}

context(c: A)
val prop1: String = <!CONTEXT_PARAMETERS_WITH_BACKING_FIELD!>""<!>

context(c: A)
var prop2: String = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>""<!>
    get() = ""
    set(value) {
        <!UNRESOLVED_REFERENCE!>field<!> = value
    }
