// TARGET_BACKEND: JVM_IR

// FILE: one/two/Bar.java
package one.two;

public class Bar {
    public static final int VIA_DEFAULT_FACADE = MainKt.FOO;
    public static final int VIA_RENAMED_FACADE = CustomKt.BAZ;

    public static int viaJava() {
        return VIA_DEFAULT_FACADE * 10 + VIA_RENAMED_FACADE;
    }
}

// FILE: other.kt
@file:JvmName("CustomKt")

package one.two

const val BAZ = 2

// FILE: main.kt
package one.two

const val FOO = 1

const val FROM_KOTLIN = Bar.VIA_DEFAULT_FACADE * 10 + Bar.VIA_RENAMED_FACADE

fun box(): String {
    val fromJava = Bar.viaJava()
    if (FROM_KOTLIN != 12) return "FAIL: kotlin=$FROM_KOTLIN"
    return if (FROM_KOTLIN == fromJava) "OK" else "FAIL: kotlin=$FROM_KOTLIN java=$fromJava"
}
