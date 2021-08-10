// !CHECK_TYPE
open class BaseOuter {
    protected fun foo() = 1
    protected fun bar() { }
}

class Foo(var base: BaseOuter)

fun BaseOuter.foo(): String = ""

class Derived : BaseOuter() {
    fun test(foo: Foo) {
        if (foo.base is Derived) {
            foo.base.foo() checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><String>() } // Resolved to extension
            foo.base.bar()
        }
    }
}
