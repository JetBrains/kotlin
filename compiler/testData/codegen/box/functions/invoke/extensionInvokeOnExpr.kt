// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
class A

class B {
    operator fun A.invoke() = "##"
    operator fun A.invoke(i: Int) = "#${i}"
}

fun foo() = A()

fun B.test(): String {
    if (A()() != "##") return "fail1"
    if (A()(1) != "#1") return "fail2"
    if (foo()() != "##") return "fail3"
    if (foo()(42) != "#42") return "fail4"
    if ((foo())(42) != "#42") return "fail5"
    if ({ -> A()}()() != "##") return "fail6"
    if ({ -> A()}()(37) != "#37") return "fail7"
    return "OK"
}

fun box(): String = B().test()

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
