// FILE: asOnPlatformType.kt
fun test() {
    val nullStr = JavaClass.nullString()
    val nonnullStr = JavaClass.nonnullString()

    nullStr.foo()
    nonnullStr.foo()
    nullStr.fooN()
    nonnullStr.fooN()
}

inline fun <reified T> T.foo(): T = this as T
inline fun <reified T> T.fooN(): T? = this as T?

// FILE: JavaClass.java
public class JavaClass {
    public static String nullString() {
        return null;
    }

    public static String nonnullString() {
        return "OK";
    }
}
