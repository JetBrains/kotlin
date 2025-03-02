// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING
// JVM_EXPOSE_BOXED

// FILE: J.java

public class J {
    public String test() {
        return StringWrapper.unwrap(new StringWrapper("OK"));
    }
}

// FILE: Box.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class StringWrapper(val s: String) {
    companion object {
        @JvmStatic
        @JvmExposeBoxed
        fun unwrap(s: StringWrapper): String = s.s
    }
}

fun box(): String {
    return J().test()
}