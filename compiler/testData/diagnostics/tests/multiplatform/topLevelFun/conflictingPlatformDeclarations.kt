// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

<!CONFLICTING_OVERLOADS!>platform fun foo()<!>
<!CONFLICTING_OVERLOADS!>platform fun foo()<!>

platform fun foo(x: Int)
