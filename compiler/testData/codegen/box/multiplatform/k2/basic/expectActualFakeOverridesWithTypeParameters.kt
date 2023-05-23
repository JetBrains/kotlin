// IGNORE_BACKEND_K1: JVM, JVM_IR, JS, JS_IR, JS_IR_ES6, NATIVE, WASM
// !LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-58004

// MODULE: common
// FILE: common.kt

expect open class A<S>() {
    fun <T> f(arg: T): T

    fun g(s: S): S

    val <S> S.p: S
}

class B : A<String>()

fun foo(arg: B) = arg.f("O")  + arg.g("K")

// MODULE: platform()()(common)
// FILE: platform.kt

actual open class A<S> {
    actual fun <T> f(arg: T) = arg

    actual fun g(s: S): S = s.p

    actual val <S> S.p: S get() = this
}

fun box() = foo(B())