// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: Baz.java

public class Baz {
    public static String baz() {
        return Util.foo() + Util.bar();
    }
}

// FILE: bar.kt

@file:JvmName("Util")
@file:JvmMultifileClass
public fun bar(): String = "K"

// FILE: foo.kt

@file:[JvmName("Util") JvmMultifileClass]
public fun foo(): String = "O"

// FILE: test.kt

fun box(): String = Baz.baz()
