// FIR_IDENTICAL
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt

expect open class Foo {
    fun existingMethod()
    val existingParam: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

@OptIn(kotlin.ExperimentalMultiplatform::class)
@kotlin.AllowDifferentMembersInActual
actual open class Foo {
    actual fun existingMethod() {}
    actual val existingParam: Int = 904

    fun injectedMethod() {} // accidential override can happen with this injected fun. That's why it's prohibited
}
