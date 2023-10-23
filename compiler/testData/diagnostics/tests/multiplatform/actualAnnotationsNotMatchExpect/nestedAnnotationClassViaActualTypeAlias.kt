// MODULE: m1-common
// FILE: common.kt

expect annotation class Ann() {
    annotation class Nested()
}

@Ann.<!UNRESOLVED_REFERENCE{JVM}!>Nested<!>
expect fun foo()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
annotation class AnnImpl {
    annotation class Nested
}

actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Ann<!> = AnnImpl

@AnnImpl.Nested
actual fun foo() {}
