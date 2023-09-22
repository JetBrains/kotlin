// MODULE: m1-common
// FILE: common.kt
expect class Foo {
    fun foo(param: Int = 1)
    fun missingOnActual()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
open class Base {
    fun foo(param: Int) {}
}

actual class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!> : Base()
