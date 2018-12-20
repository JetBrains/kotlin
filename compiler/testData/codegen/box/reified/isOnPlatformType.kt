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

    if (nullStr.foo() != true) return "fail 1"
    if (nonnullStr.foo() != true) return "fail 2"

    if (nullStr.fooN() != true) return "fail 3"
    if (nonnullStr.fooN() != true) return "fail 4"

    return "OK"
}

inline fun <reified T> T.foo(): Boolean = this is T

inline fun <reified T> T.fooN(): Boolean = this is T?
