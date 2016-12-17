// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

header enum class En<!HEADER_ENUM_CONSTRUCTOR!>(x: Int)<!> {
    E1,
    E2(42),
    ;

    <!HEADER_ENUM_CONSTRUCTOR!>constructor(s: String)<!>
}

header enum class En2 {
    E1<!NO_CONSTRUCTOR!>()<!>
}
