// MODULE: m1-common
// FILE: common.kt

expect class B {
    class N {
        <!EXPECTED_DECLARATION_WITH_BODY, EXPECTED_DECLARATION_WITH_BODY{JVM}!>fun body()<!> {}
        <!WRONG_MODIFIER_TARGET, WRONG_MODIFIER_TARGET{JVM}!>expect<!> fun extraHeader()
    }
}

expect class C {
    <!WRONG_MODIFIER_TARGET, WRONG_MODIFIER_TARGET{JVM}!>expect<!> class N
    <!WRONG_MODIFIER_TARGET, WRONG_MODIFIER_TARGET{JVM}!>expect<!> enum class E
    <!WRONG_MODIFIER_TARGET, WRONG_MODIFIER_TARGET{JVM}!>expect<!> inner class I
}

expect class D {
    class N
}

expect class E {
    class N
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

actual class B {
    actual class N {
        actual fun body() {}
        actual fun extraHeader() {}
    }
}

actual class C {
    actual class N
    actual enum class E
    actual inner class I
}

actual class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>D<!>

actual class E {
    class <!ACTUAL_MISSING!>N<!>
}
