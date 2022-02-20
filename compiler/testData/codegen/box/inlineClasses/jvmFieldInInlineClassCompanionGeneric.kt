// TARGET_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

// FILE: test.kt

OPTIONAL_JVM_INLINE_ANNOTATION
value class R<T: Int>(private val r: T) {

    companion object {
        @JvmField
        val ok = "OK"
    }
}

fun box() = J.test()

// FILE: J.java
public class J {
    public static String test() {
        return R.ok;
    }
}