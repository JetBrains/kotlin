// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect open class Foo {
    fun existingMethod()
    val existingParam: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

open class InjectedEmptySuperClass()

actual open class Foo : InjectedEmptySuperClass() {
    actual fun existingMethod() {}
    actual val existingParam: Int = 904
}
