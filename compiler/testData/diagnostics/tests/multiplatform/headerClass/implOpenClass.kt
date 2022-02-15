// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect class Foo

expect fun getFoo(): Foo

fun <T : Foo> bar() {} // no "Foo is final" warning should be here

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo

class Bar : Foo()

actual fun getFoo(): Foo = Bar()
