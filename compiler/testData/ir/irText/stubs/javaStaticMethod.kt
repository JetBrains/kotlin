// !DUMP_DEPENDENCIES
// FILE: J.java

public class J {
    public static void bar() {}
}

// FILE: javaStaticMethod.kt

fun test() = J.bar()