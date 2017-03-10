// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME
// FILE: Baz.java

public class Baz {
    public static String baz() {
        return Foo.foo() + Bar.bar();
    }
}

// FILE: bar.kt

@file:JvmName("Bar")
public fun bar(): String = "K"

// FILE: foo.kt

@file:JvmName("Foo")
public fun foo(): String = "O"

// FILE: test.kt

fun box(): String = Baz.baz()
