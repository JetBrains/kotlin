// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// FILE: Child.java

class Child extends Parent {
    public static int b = 3;
    public static int c = 4;
}

// FILE: Parent.java

class Parent {
    public static int a = 1;
    public static int b = 2;
}

// FILE: test.kt

fun box(): String {
    if (Parent.a != 1) return "expected Parent.a == 1"
    if (Parent.b != 2) return "expected Parent.b == 2"
    if (Child.a != 1) return "expected Child.a == 1"
    if (Child.b != 3) return "expected Child.b == 3"
    if (Child.c != 4) return "expected Child.c == 4"

    return "OK"
}
