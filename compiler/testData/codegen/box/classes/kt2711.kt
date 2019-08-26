// IGNORE_BACKEND: WASM
class IntRange {
    operator fun contains(a: Int) = (1..2).contains(a)
}

class C() {
    operator fun rangeTo(i: Int) = IntRange()
}


fun box(): String {
    if (2 in C()..2) {
        2 == 2
    }
    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ .. 
