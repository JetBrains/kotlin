// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect enum class En(x: Int) {
    <!NONE_APPLICABLE!>E1,<!>
    E2(42),
    ;

    <!NONE_APPLICABLE!>constructor(s: String)<!>
}

expect enum class En2 {
    E1()
}
