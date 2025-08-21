// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING
// LANGUAGE: +ImplicitJvmExposeBoxed

// FILE: J.java

public class J {
    public String test() {
        return StringWrapper.unwrap(new StringWrapper("OK"));
    }
}

// FILE: Box.kt

@JvmInline
value class StringWrapper(val s: String) {
    companion object {
        @JvmStatic
        fun unwrap(s: StringWrapper): String = s.s
    }
}

fun box(): String {
    return J().test()
}