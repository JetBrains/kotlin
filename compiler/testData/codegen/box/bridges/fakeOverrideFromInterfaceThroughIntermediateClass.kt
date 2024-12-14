// LANGUAGE: -AbstractClassMemberNotImplementedWithIntermediateAbstractClass
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, JS_IR_ES6, NATIVE, WASM
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// FIR status: don't support legacy feature
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.

interface A {
    fun foo(): Any
}

interface B {
    fun foo(): String = "A"
}

open class D: B

open class C: D(), A

fun box(): String {
    val a: A = C()
    if (a.foo() != "A") return "Fail 1"
    if ((a as B).foo() != "A") return "Fail 2"
    if ((a as C).foo() != "A") return "Fail 3"
    return "OK"
}
