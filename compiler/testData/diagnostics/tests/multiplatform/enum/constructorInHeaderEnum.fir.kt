// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect enum class En(x: Int) {
    <!INAPPLICABLE_CANDIDATE!>E1,<!>
    E2(42),
    ;

    <!INAPPLICABLE_CANDIDATE!>constructor(s: String)<!>
}

expect enum class En2 {
    E1()
}
