// !LANGUAGE: +InlineClasses
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

// FILE: test.kt
inline class R(private val r: Int) {

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