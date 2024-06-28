// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

// Rules for expect actual matching are ad-hoc for nested classes. That's why this test exist
expect class Outer {
    open class Foo {
        fun existingMethod()
        val existingParam: Int
    }
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class Outer {
    actual open class Foo {
        actual fun existingMethod() {}
        actual val existingParam: Int = 904

        fun injectedMethod() {}
    }
}
