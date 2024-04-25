// LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion

open class Base {
    companion object {
        annotation class Foo
    }
}

class Derived : Base() {

    @<!DEPRECATED_ACCESS_BY_SHORT_NAME!>Foo<!>
    fun foo() = 42
}
