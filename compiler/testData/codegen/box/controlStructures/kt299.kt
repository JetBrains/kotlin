// IGNORE_BACKEND: WASM
class MyRange1() : ClosedRange<Int> {
    override val start: Int
        get() = 0
    override val endInclusive: Int
        get() = 0
    override fun contains(item: Int) = true
}

class MyRange2() {
    operator fun contains(item: Int) = true
}

fun box(): String {
    if (1 in MyRange1()) {
        if (1 in MyRange2()) {
            return "OK"
        }
        return "fail 2"
    }
    return "fail 1"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ ClosedRange 
