// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt

expect open class Base {
    fun injected()
}

expect open class Foo {
    fun existingMethod()
    val existingParam: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Base {
    actual fun injected() {}
}

actual open class Foo : Base() {
    actual fun existingMethod() {}
    actual val existingParam: Int = 904
}