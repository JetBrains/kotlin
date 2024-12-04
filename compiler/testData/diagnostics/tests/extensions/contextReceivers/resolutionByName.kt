// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers
class Foo

context(Foo, label@Int)
fun foo() {
    <!NO_COMPANION_OBJECT!>Foo<!>
    <!UNRESOLVED_REFERENCE!>label<!>
}

context(Foo, label@Int)
val bar: String
    get() {
        <!NO_COMPANION_OBJECT!>Foo<!>
        <!UNRESOLVED_REFERENCE!>label<!>
        return ""
    }