// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

// FILE: A.java
public class A {
    public A getFoo() { return 3; }
}

// FILE: B.java
public class B {
    public B getFoo() { return ""; }
}

// FILE: 1.kt
class C(val foo: C)

fun test(a: A, b: B, c: C) {
    with(a) {
        with(b) {
            foo checkType { _<B>() }
        }
        foo checkType { _<A>() }
    }

    with(a) {
        with(c) {
            foo checkType { _<C>() }
        }
    }

    with(c) {
        with(a) {
            foo checkType { _<A>() }
        }
    }
}
