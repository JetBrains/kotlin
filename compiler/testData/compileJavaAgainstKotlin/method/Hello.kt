// FILE: Hello.java
package test;


class Hello {
    public static void xx() {
        String s = HelloKt.f();
    }
}

// FILE: Hello.kt
package test

fun f() = "hello"
