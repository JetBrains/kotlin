// TARGET_BACKEND: JVM

// FILE: J.java

public class J {
    // This test checks that although type '@org.jetbrains.annotations.NotNull Integer' is perceived as simple Int,
    // it's correctly mapped to 'Lj.l.Integer' by JVM backend
    public static String test(@org.jetbrains.annotations.NotNull Integer x) {
        return "OK";
    }
}

// FILE: box.kt

fun box() = J.test(1)
