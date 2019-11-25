// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME

// FILE: test.kt
inline class R(private val r: Int) {

    companion object {
        val ok
            @JvmStatic get() = "OK"
    }
}

fun box() = J.test()

// FILE: J.java
public class J {
    public static String test() {
        return R.getOk();
    }
}