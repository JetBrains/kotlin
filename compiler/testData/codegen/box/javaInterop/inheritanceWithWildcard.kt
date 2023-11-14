// TARGET_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS
// FILE: A.java

interface A {
    X<? extends A> foo();

    interface X<T extends A> {}
}

interface B extends A {
    @Override
    Y<? extends B> foo();

    interface Y<U extends B> extends X<U> {}
}

class BImpl implements B {
    @Override
    public Y<? extends B> foo() { return null; }
}

// FILE: 1.kt

private class D : A, BImpl()

fun box(): String =
    if (D().foo() == null) "OK" else "Fail"
