@file:Suppress("ACTUAL_WITHOUT_EXPECT")

package foo

actual interface A {
    actual fun commonFun()
    actual val b: B
    actual fun bFun(): B
    fun platformFun()
}

actual interface B {
    actual fun commonFunB()
    fun platformFunB()
}
