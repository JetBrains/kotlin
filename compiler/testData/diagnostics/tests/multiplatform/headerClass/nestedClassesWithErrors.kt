// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

header class B {
    class N {
        <!HEADER_DECLARATION_WITH_BODY, JVM:HEADER_DECLARATION_WITH_BODY!>fun body()<!> {}
        <!WRONG_MODIFIER_TARGET, JVM:WRONG_MODIFIER_TARGET!>header<!> fun extraHeader()
    }
}

header class C {
    <!WRONG_MODIFIER_TARGET, JVM:WRONG_MODIFIER_TARGET!>header<!> class N
    <!WRONG_MODIFIER_TARGET, JVM:WRONG_MODIFIER_TARGET!>header<!> enum class E
    <!WRONG_MODIFIER_TARGET, JVM:WRONG_MODIFIER_TARGET!>header<!> inner class I
}

header class D {
    class N
}

// MODULE: m1-jvm(m1-common)
// FILE: jvm.kt

impl class B {
    impl class N {
        impl fun body() {}
        impl fun extraHeader() {}
    }
}

impl class C {
    impl class N
    impl enum class E
    impl inner class I
}

impl class <!HEADER_CLASS_MEMBERS_ARE_NOT_IMPLEMENTED!>D<!>
