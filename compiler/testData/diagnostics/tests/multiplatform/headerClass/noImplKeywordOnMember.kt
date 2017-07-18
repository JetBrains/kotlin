// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

header class Foo {
    fun bar(): String
}

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

// TODO: run HeaderImplDeclarationChecker on non-impl members of impl classes, and report something like "impl expected" on 'bar' instead
impl class <!HEADER_CLASS_MEMBERS_ARE_NOT_IMPLEMENTED!>Foo<!> {
    fun bar(): String = "bar"
}
