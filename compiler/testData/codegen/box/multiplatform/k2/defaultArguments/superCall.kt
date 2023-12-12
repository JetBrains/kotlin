// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: EXPECT_DEFAULT_PARAMETERS
// LANGUAGE: +MultiPlatformProjects
// K2/NATIVE fail: KT-59608
// IGNORE_BACKEND: NATIVE, JVM, JVM_IR

// MODULE: common
// FILE: common.kt
package foo

expect open class A {
    open fun foo(x: Int = 20, y: Int = 3): Int
}

// MODULE: platform()()(common)
// FILE: main.kt
package foo

actual open class A {
    actual open fun foo(x: Int, y: Int) = x + y
}

open class B : A() {
    override fun foo(x: Int, y: Int) = 0

    fun bar1() = super.foo()

    fun bar2() = super.foo(30)

    fun bar3() = super.foo(y = 4)
}

fun box(): String {
    val v1 = B().bar1()
    if (v1 != 23) return "fail1: $v1"

    val v2 = B().bar2()
    if (v2 != 33) return "fail2: $v2"

    val v3 = B().bar3()
    if (v3 != 24) return "fail3: $v3"

    return "OK"
}
