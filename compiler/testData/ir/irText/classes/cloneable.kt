// FIR_IDENTICAL
// SKIP_KLIB_TEST
// IGNORE_BACKEND_K1: JS_IR
// IGNORE_BACKEND_K1: JS_IR_ES6
// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6
// STATUS: This should not work in JS, Cloneable is JVM-specific API

class A : Cloneable

interface I : Cloneable

class C : I

class OC : I {
    override fun clone(): OC = OC()
}
