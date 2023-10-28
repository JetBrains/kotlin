// LANGUAGE: +MultiPlatformProjects
// It accidentially passes K1 JVM while not intended to do so.
// IGNORE_BACKEND_K1: JS, JS_IR, JS_IR_ES6, WASM
// IGNORE_NATIVE_K1: mode=ONE_STAGE_MULTI_MODULE
// MODULE: common
// FILE: common.kt

open class A {
    open internal fun foo() = "A"
}

open class B {
    open internal fun foo() = "B"
}


// MODULE: middle()()(common)
// FILE: middle.kt

class AChild : A() {
    override public fun foo() = "AChild"
}

open class C {
    open internal fun foo() = "C"
}

// MODULE: main()()(middle)
// FILE: main.kt

class BChild : B() {
    override public fun foo() = "BChild"
}

class CChild : C() {
    override public fun foo() = "CChild"
}


fun box() : String {
    val a: A = AChild()
    val b: B = BChild()
    val c: C = CChild()
    if (a.foo() != "AChild") return "Fail 1"
    if (b.foo() != "BChild") return "Fail 2"
    if (c.foo() != "CChild") return "Fail 3"
    return "OK"
}