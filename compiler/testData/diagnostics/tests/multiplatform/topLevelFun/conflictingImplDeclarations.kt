// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

header fun foo()

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

<!CONFLICTING_OVERLOADS!>impl fun foo()<!> {}
<!CONFLICTING_OVERLOADS!>impl fun foo()<!> {}

// MODULE: m3-js(m1-common)
// FILE: js.kt

<!CONFLICTING_OVERLOADS!>impl fun foo()<!> {}
<!CONFLICTING_OVERLOADS!>impl fun foo()<!> {}
