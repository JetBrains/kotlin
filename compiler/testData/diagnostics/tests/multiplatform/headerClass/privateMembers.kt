// MODULE: m1-common
// FILE: common.kt

expect class A private constructor() {
    <!EXPECTED_PRIVATE_DECLARATION, EXPECTED_PRIVATE_DECLARATION{JVM}!>private<!> fun foo()
    <!EXPECTED_PRIVATE_DECLARATION, EXPECTED_PRIVATE_DECLARATION{JVM}!>private<!> val bar: String
    <!EXPECTED_PRIVATE_DECLARATION, EXPECTED_PRIVATE_DECLARATION{JVM}!>private<!> fun Int.memExt(): Any

    <!EXPECTED_PRIVATE_DECLARATION, EXPECTED_PRIVATE_DECLARATION{JVM}!>private<!> class Nested

    var baz: Any?
        private set
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

actual class A actual private constructor() {
    private fun <!ACTUAL_MISSING!>foo<!>() {}
    private val <!ACTUAL_MISSING!>bar<!>: String = ""
    actual private fun Int.memExt(): Any = 0

    actual private class Nested

    actual var baz: Any? = null
        private set
}
