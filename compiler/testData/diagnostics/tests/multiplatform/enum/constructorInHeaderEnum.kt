// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect enum class En<!EXPECTED_ENUM_CONSTRUCTOR!>(x: Int)<!> {
    E1,
    E2(42),
    ;

    <!EXPECTED_ENUM_CONSTRUCTOR!>constructor(s: String)<!>
}

expect enum class En2 {
    E1<!NO_CONSTRUCTOR!>()<!>
}
