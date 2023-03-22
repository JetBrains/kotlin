// IGNORE_REVERSED_RESOLVE
// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java
public class A {
    static byte foo = 1;
    static int bar = 2;
}

// FILE: B.java
public class B extends A {}

// FILE: C.java
public class C {
    static long bar = 3;
}

// FILE: 1.kt
import A.foo
import B.bar

class E: A() {
    init {
        foo
        bar
    }
}

class F: B() {
    init {
        foo
        bar
    }
}

// FILE: 2.kt
import C.bar

class Z: A() {
    init {
        val a: Int = bar
    }
}

// FILE: 3.kt
import C.*

class Q: A() {
    init {
        val a: Int = bar
    }
}

// FILE: 4.kt
val bar = ""

class W: A() {
    init {
        val a: Int = bar
    }
}