// TARGET_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

// FILE: test.kt

OPTIONAL_JVM_INLINE_ANNOTATION
value class R(private val r: Int) {

    companion object {
        private var ok_ = ""

        @JvmStatic
        var ok
            get() = ok_
            set(value) { ok_ = value }
    }
}

fun box() = J.test()

// FILE: J.java
public class J {
    public static String test() {
        R.setOk("OK");
        return R.getOk();
    }
}