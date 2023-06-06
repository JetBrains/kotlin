// MODULE: m1-common
// FILE: common.kt

expect class B {
    class N {
        <!EXPECTED_DECLARATION_WITH_BODY!>fun body()<!> {}
        <!WRONG_MODIFIER_TARGET!>expect<!> fun extraHeader()
    }
}

expect class C {
    <!WRONG_MODIFIER_TARGET!>expect<!> class N
    <!WRONG_MODIFIER_TARGET!>expect<!> enum class E
    <!WRONG_MODIFIER_TARGET!>expect<!> inner class I
}

<!INCOMPATIBLE_MATCHING{JVM}!>expect class D {
    <!NO_ACTUAL_FOR_EXPECT{JVM}!>class N<!>
}<!>

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
    class N
}
