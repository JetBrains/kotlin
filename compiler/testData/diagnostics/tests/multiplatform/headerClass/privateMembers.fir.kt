// MODULE: m1-common
// FILE: common.kt

expect class A private constructor() {
    <!EXPECTED_PRIVATE_DECLARATION!>private<!> fun foo()
    <!EXPECTED_PRIVATE_DECLARATION!>private<!> val bar: String
    <!EXPECTED_PRIVATE_DECLARATION!>private<!> fun Int.memExt(): Any

    <!EXPECTED_PRIVATE_DECLARATION!>private<!> class Nested

    var baz: Any?
        private set
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

actual class A actual private constructor() {
    private fun foo() {}
    private val bar: String = ""
    actual private fun Int.memExt(): Any = 0

    actual private class Nested

    actual var baz: Any? = null
        private set
}
