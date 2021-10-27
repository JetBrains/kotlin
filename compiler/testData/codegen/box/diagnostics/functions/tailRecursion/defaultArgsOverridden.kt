// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR_ES6
// IGNORE_FIR_DIAGNOSTICS_DIFF

open class A {
    open fun foo(s: String = "OK") = s
}

class B : A() {
    override <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo(s: String): String {
        return if (s == "OK") s else foo()
    }
}

fun box() = B().foo("FAIL")
