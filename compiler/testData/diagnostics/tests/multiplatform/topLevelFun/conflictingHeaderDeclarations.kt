// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

<!CONFLICTING_OVERLOADS!>header fun foo()<!>
<!CONFLICTING_OVERLOADS!>header fun foo()<!>

header fun foo(x: Int)
