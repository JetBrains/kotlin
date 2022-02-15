// MODULE: m1-common
// FILE: common.kt

expect class A {
    <!EXPECTED_PRIVATE_DECLARATION, EXPECTED_PRIVATE_DECLARATION{JVM}!>private<!> fun foo()
    <!EXPECTED_PRIVATE_DECLARATION, EXPECTED_PRIVATE_DECLARATION{JVM}!>private<!> val bar: String
    <!EXPECTED_PRIVATE_DECLARATION, EXPECTED_PRIVATE_DECLARATION{JVM}!>private<!> fun Int.memExt(): Any

    <!EXPECTED_PRIVATE_DECLARATION, EXPECTED_PRIVATE_DECLARATION{JVM}!>private<!> class Nested
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

actual class A {
    private fun <!ACTUAL_MISSING!>foo<!>() {}
    private val <!ACTUAL_MISSING!>bar<!>: String = ""
    actual private fun Int.memExt(): Any = 0

    actual private class Nested
}
