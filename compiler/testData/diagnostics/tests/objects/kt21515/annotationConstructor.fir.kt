// !LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion

open class Base {
    companion object {
        annotation class Foo
    }
}

class Derived : Base() {

    @Foo
    fun foo() = 42
}