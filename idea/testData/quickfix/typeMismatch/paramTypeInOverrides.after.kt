// "Change parameter 'a' type of function 'test.B.foo' to 'String'" "true"
// DISABLE-ERRORS
package test

open class B {
    open fun foo(a: String) {}
}

class C : B() {
    override fun foo(a: String) = super.foo(a)
}

fun test(b: B) {
    b.foo("")
}