// FIR_IDENTICAL
// SKIP_KLIB_TEST
// IGNORE_BACKEND_K1: JS_IR
// IGNORE_BACKEND_K1: JS_IR_ES6

class A : Cloneable

interface I : Cloneable

class C : I

class OC : I {
    override fun clone(): OC = OC()
}
