// TARGET_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

// FILE: a.kt

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC<T: Int>(val v: T) {
    fun <T> getT(): T? = null
}

// FILE: UseIC.java

public class UseIC {
    private IC ic = null;

    public static String result() {
        return "OK";
    }
}

// FILE: test.kt

fun box(): String {
    return UseIC.result()
}
