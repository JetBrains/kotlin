// KOTLIN_CONFIGURATION_FLAGS: +JVM.DISABLE_PARAM_ASSERTIONS
// FILE: noAssertionsForKotlin.kt

class A {
    val x: Int = 42

    fun foo(): String = ""

    companion object {
        val y: Any? = 239

        fun bar(): String = ""
    }
}

fun baz(): String = ""

// FILE: noAssertionsForKotlinMain.kt

fun bar() {
    val x = A().x
    val foo = A().foo()
    val y = A.y
    val bar = A.bar()
    val baz = baz()
}

// @A.class:
// 0 kotlin/jvm/internal/Intrinsics
// @NoAssertionsForKotlinKt.class:
// 0 kotlin/jvm/internal/Intrinsics
// @NoAssertionsForKotlinMainKt.class:
// 0 kotlin/jvm/internal/Intrinsics
