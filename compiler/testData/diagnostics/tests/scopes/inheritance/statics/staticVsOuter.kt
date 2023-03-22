// IGNORE_REVERSED_RESOLVE
// FIR_IDENTICAL
// FILE: A.java
public class A {
    public static int foo() {return 1;}
}

// FILE: B.java
public class B extends A {
    public static int foo() {return 1;}
}

// FILE: 1.kt

fun foo() = ""

class C: B() {
    init {
        val a: Int = foo()
    }
}
