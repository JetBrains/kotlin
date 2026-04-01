// TARGET_BACKEND: JVM
// FILE: JavaClass.java
public class JavaClass {
    public static String foo(Integer x, String y) { return y; }
}

// FILE: 1.kt
fun bar(x: Int.(y: String) -> String): String {
    return 1.x("OK")
}

fun box(): String {
    return bar(JavaClass::foo)
}