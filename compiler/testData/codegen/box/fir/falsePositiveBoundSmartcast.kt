abstract class A {
    abstract fun foo(): String
}

class B : A() {
    override fun foo() = "OK"
}

class C : A() {
    override fun foo() = "fail"
}

fun test(c: C, cond: Boolean): String {
    var x: A = c
    if (cond) {
        x = B()
    }
    return x.foo()
}

fun box(): String = test(C(), true)
