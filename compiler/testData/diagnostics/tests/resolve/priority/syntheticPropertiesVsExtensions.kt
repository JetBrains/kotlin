// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

// FILE: A.java
public class A {
    public A getFoo() { return 3; }
}

// FILE: 1.kt

private val A.foo: Int get() = 4

fun test(a: A) {
    a.foo checkType { _<A>() }
    with(a) {
        foo checkType { _<A>() }
    }
}

class B {
    private val A.foo: B get() = this@B

    fun test(a: A) {
        a.foo checkType { _<A>() }
        with(a) {
            foo checkType { _<A>() }
        }
    }
}
