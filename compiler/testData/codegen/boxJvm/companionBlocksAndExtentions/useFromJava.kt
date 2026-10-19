// LANGUAGE: +CompanionBlocks
// WITH_STDLIB

// FILE: a.kt

class C {
    companion {
        val basicVal = "C::basicVal"
        fun foo() = "C::foo"
    }
}

@JvmInline
value class I(val value: String) {
    companion {
        val basicVal = "I::basicVal"
        fun foo() = "I::foo"
    }
}

// FILE: B.java

public class B {
    public static String test() {
        if (C.getBasicVal() != "C::basicVal") return "fail: C::basicVal";
        if (C.foo() != "C::foo") return "fail: C::foo";
        if (I.getBasicVal() != "I::basicVal") return "fail: I::basicVal";
        if (I.foo() != "I::foo") return "fail: I::foo";
        return "OK";
    }
}

// FILE: test.kt

fun box() = B.test()
