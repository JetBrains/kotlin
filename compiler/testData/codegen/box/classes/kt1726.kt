class Foo(
        var state : Int,
        val f : (Int) -> Int){

    fun next() : Int {
        val nextState = f(state)
        state = nextState
        return state
    }
}

fun box(): String {
    val f = Foo(23, {x -> 2 * x})
    return if (f.next() == 46) "OK" else "fail"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
