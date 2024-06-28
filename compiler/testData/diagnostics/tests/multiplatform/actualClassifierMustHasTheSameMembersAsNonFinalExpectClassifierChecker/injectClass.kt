// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect open class Foo {
    fun existingFun()
    val existingParam: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo {
    actual fun existingFun() {}
    actual val existingParam: Int = 904

    class InjectedClass

    // Injected classes can be considered as members (because they caputer `this`) => scopes are different
    // => the diagnostic should be reported.
    //
    // But since `override inner class` isn't possible in Kotlin, red code here is unnecessary
    inner class InjectedInnerClass
}
