// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

<!DEPRECATED_MODIFIER, JVM:DEPRECATED_MODIFIER!>header<!> class My

<!DEPRECATED_MODIFIER, JVM:DEPRECATED_MODIFIER!>header<!> fun foo(): Int

<!DEPRECATED_MODIFIER, JVM:DEPRECATED_MODIFIER!>header<!> val x: String

<!DEPRECATED_MODIFIER, JVM:DEPRECATED_MODIFIER!>header<!> object O

<!DEPRECATED_MODIFIER, JVM:DEPRECATED_MODIFIER!>header<!> enum class E {
    FIRST
}

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

<!DEPRECATED_MODIFIER!>impl<!> class My

<!DEPRECATED_MODIFIER!>impl<!> fun foo() = 42

<!DEPRECATED_MODIFIER!>impl<!> val x get() = "Hello"

<!DEPRECATED_MODIFIER!>impl<!> object O

<!DEPRECATED_MODIFIER!>impl<!> enum class E {
    FIRST
}