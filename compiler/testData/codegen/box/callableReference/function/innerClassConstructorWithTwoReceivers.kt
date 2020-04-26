abstract class A {
    inner class InnerInA {
        fun returnOk() = "OK"
    }
}

class B : A()

fun foo(a: A): String {
    if (a is B) {
        val v = a::InnerInA
        return v().returnOk()
    }

    return "error"
}

fun box(): String {
    return foo(B())
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: BINDING_RECEIVERS
