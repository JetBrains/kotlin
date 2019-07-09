// TARGET_BACKEND: JVM

// FILE: JavaClass.java

public class JavaClass {

    public static String nullString() {
        return null;
    }

    public static String nonnullString() {
        return "OK";
    }

}

// FILE: kotlin.kt

fun box(): String {
    val nullStr = JavaClass.nullString()
    val nonnullStr = JavaClass.nonnullString()

    if (nullStr.foo() != null) return "fail 1"
    if (nonnullStr.foo() != nonnullStr) return "fail 2"

    if (nullStr.fooN() != null) return "fail 3"
    if (nonnullStr.fooN() != nonnullStr) return "fail 4"

    return "OK"
}

inline fun <reified T> T.foo(): T = this as T

inline fun <reified T> T.fooN(): T? = this as T?
