// TARGET_BACKEND: JVM
// LANGUAGE: +CompanionBlocks

// FILE: A.java
public class A {
    public static String foo() {
        return "A::foo";
    }
}

// FILE: K.kt
open class B : A() {
    companion {
        fun foo(): String = "B::foo"
        val prop: String by lazy { "B" }
        val prop2: Int by lazy { 42 }
    }
}

open class B2 : B() {
    companion {
        val prop: String get() = "B2"
        val prop2: Int get() = 43
    }
}

open class B3 : B2() {
    companion {
        var prop = 44
        var prop2 = "B3"
    }
}

open class B4 : B3() {
    companion {
        var prop = 45
        var prop2 = "B4"
    }
}

// FILE: C.java
public class C extends B3 {}

// FILE: main.kt
fun box(): String {
    if (A.foo() != "A::foo") return "fail: A.foo() == ${A.foo()} != A::foo"
    if (B.foo() != "B::foo") return "fail: B.foo() == ${B.foo()} != B::foo"
    if (C.foo() != "B::foo") return "fail: C.foo() == ${C.foo()} != B::foo"

    if (B.prop != "B") return "fail: B.prop == ${B.prop} != B"
    if (B.prop2 != 42) return "fail: B.prop2 == ${B.prop2} != 42"

    if (B2.prop != "B2") return "fail: B2.prop == ${B2.prop} != B2"
    if (B2.prop2 != 43) return "fail: B2.prop2 == ${B2.prop2} != 43"

    B3.prop = -44
    B3.prop2 = "B3_2"

    if (B3.prop != -44) return "fail: B3.prop == ${B3.prop} != -44"
    if (B3.prop2 != "B3_2") return "fail: B3.prop2 == ${B3.prop2} != B3_2"

    B4.prop = -45
    B4.prop2 = "B4_2"

    if (B4.prop != -45) return "fail: B4.prop == ${B4.prop} != -45"
    if (B4.prop2 != "B4_2") return "fail: B4.prop2 == ${B4.prop2} != B4_2"

    return "OK"
}
