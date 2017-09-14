// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

header class H {
    <!WRONG_MODIFIER_TARGET, JVM:WRONG_MODIFIER_TARGET!>header<!> fun foo()
}

// MODULE: m1-jvm(m1-common)
// FILE: jvm.kt

impl class H {
    impl fun foo() {}
}
