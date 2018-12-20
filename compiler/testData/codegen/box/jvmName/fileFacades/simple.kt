// TARGET_BACKEND: JVM

// WITH_RUNTIME
// FILE: Bar.java

public class Bar {
    public static String bar() {
        return Foo.foo();
    }
}

// FILE: foo.kt

@file:JvmName("Foo")
public fun foo(): String = "OK"

// FILE: simple.kt

fun box(): String = Bar.bar()
