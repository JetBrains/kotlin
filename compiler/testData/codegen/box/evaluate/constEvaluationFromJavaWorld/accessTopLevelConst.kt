// TARGET_BACKEND: JVM

// FILE: one/two/Bar.java
package one.two;

public class Bar {
    public static final int BAR = MainKt.FOO + 1;
}

// FILE: Main.kt
package one.two

const val FOO = 1

const val BAZ = Bar.BAR + 1

fun box(): String {
    return "OK"
}
