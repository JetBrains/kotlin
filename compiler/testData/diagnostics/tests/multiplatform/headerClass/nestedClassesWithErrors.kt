// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect class B {
    class N {
        <!EXPECTED_DECLARATION_WITH_BODY, JVM:EXPECTED_DECLARATION_WITH_BODY!>fun body()<!> {}
        <!JVM:WRONG_MODIFIER_TARGET, WRONG_MODIFIER_TARGET!>expect<!> fun extraHeader()
    }
}

expect class C {
    <!JVM:WRONG_MODIFIER_TARGET, WRONG_MODIFIER_TARGET!>expect<!> class N
    <!JVM:WRONG_MODIFIER_TARGET, WRONG_MODIFIER_TARGET!>expect<!> enum class E
    <!JVM:WRONG_MODIFIER_TARGET, WRONG_MODIFIER_TARGET!>expect<!> inner class I
}

expect class D {
    class N
}

expect class E {
    class N
}

// MODULE: m1-jvm(m1-common)
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
