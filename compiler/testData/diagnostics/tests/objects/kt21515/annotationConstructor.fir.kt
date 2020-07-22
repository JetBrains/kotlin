// !LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion

open class Base {
    companion object {
        annotation class Foo
    }
}

class Derived : Base() {

    @<!OTHER_ERROR!>Foo<!>
    fun foo() = 42
}