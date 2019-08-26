// IGNORE_BACKEND: WASM
// KT-9277 Unexpected NullPointerException in an invocaton with named arguments

fun box(): String {
    foo(null)

    return "OK"
}

fun foo(x : Int?){
    bar(z = x ?: return, y = x)
}

fun bar(y : Int, z : Int) {}


// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: UNIT