// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect fun foo(x: Int): Int

fun callFromCommonCode(x: Int) = foo(x)

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual fun foo(x: Int): Int {
    return x + 1
}

fun callFromJVM(x: Int) = foo(x)

// MODULE: m3-js()()(m1-common)
// FILE: js.kt

actual fun foo(x: Int): Int {
    return x - 1
}

fun callFromJS(x: Int) = foo(x)
