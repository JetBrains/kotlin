// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6
//   KT-65822: JS targets are ignored, as they doesn't support source-binary-source dependencies
// IGNORE_BACKEND_K2: WASM
//   Reason: KT-65794
// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE && target=linux_x64
// ISSUE: KT-65669

// MODULE: a
// FILE: a.kt
abstract class Base {
    open fun foo() {}
}

open class Derived : Base()

// MODULE: b(a)
// FILE: first.kt
class Impl : Derived() {
    override fun foo() {}
}

// FILE: second.kt
abstract class Base {
    open fun foo() {}
}

fun box(): String = "OK"
