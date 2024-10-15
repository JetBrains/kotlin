// RUN_PIPELINE_TILL: SOURCE
// LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion

open class Base {
    companion object {
        annotation class Foo
    }
}

class Derived : Base() {

    @<!UNRESOLVED_REFERENCE!>Foo<!>
    fun foo() = 42
}
