// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect class B {
    class N {
        <!HEADER_DECLARATION_WITH_BODY, JVM:HEADER_DECLARATION_WITH_BODY!>fun body()<!> {}
        <!WRONG_MODIFIER_TARGET, JVM:WRONG_MODIFIER_TARGET!>expect<!> fun extraHeader()
    }
}

expect class C {
    <!WRONG_MODIFIER_TARGET, JVM:WRONG_MODIFIER_TARGET!>expect<!> class N
    <!WRONG_MODIFIER_TARGET, JVM:WRONG_MODIFIER_TARGET!>expect<!> enum class E
    <!WRONG_MODIFIER_TARGET, JVM:WRONG_MODIFIER_TARGET!>expect<!> inner class I
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

actual class <!HEADER_CLASS_MEMBERS_ARE_NOT_IMPLEMENTED!>D<!>

actual class E {
    class <!IMPL_MISSING!>N<!>
}
