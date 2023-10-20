// MODULE: m1-common
// FILE: common.kt

expect class Foo {
    fun bar(): String
    fun bas(f: Int)
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!> {
    fun bar(): String = "bar"
    fun bas(g: Int) {}
}
