// FILE: A.java
public class A {
    public static void foo() {}
}

// FILE: B.java
public class B extends A {
    public static void bar() {}
}

// FILE: 1.kt
open class X: A() {
    init {
        foo()
        A.foo()
    }
}

open class Y: B() {
    init {
        foo()
        A.foo()
        B.foo()

        bar()
        B.bar()
    }
}

class XN: X() {
    init {
        foo()
        A.foo()
        X.foo()
        XN.foo()
    }
}

class YN: Y() {
    init {
        foo()
        A.foo()
        Y.foo()
        YN.foo()

        bar()
        B.bar()
        Y.bar()
        YN.bar()
    }
}
