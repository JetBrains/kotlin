// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

class A

class B {
    fun foo() = this
}

fun test(foo: A.() -> Int) {
    with(A()) {
        foo() checkType { _<Int>() }
        with(B()) {
            foo() checkType { _<B>() }
            this.foo() checkType { _<B>() }
        }
    }
}
