// FIR_IDENTICAL
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt

expect open class Base() {
    open fun existingMethodInBase()
}

expect open class Foo : Base {
    fun existingMethod()
    val existingParam: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Base {
    actual open fun existingMethodInBase() {}
}

actual open class Foo : Base() {
    actual fun existingMethod() {}
    actual val existingParam: Int = 904

    override fun existingMethodInBase() {} // override from super
}
