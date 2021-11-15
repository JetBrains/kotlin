// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: test.kt
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class R(private val r: Int) {

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