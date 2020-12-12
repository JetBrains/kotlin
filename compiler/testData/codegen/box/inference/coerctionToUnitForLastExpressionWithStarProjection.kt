// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: UNKNOWN
class Inv<T>

fun Inv<*>.invToInv(): Inv<*>? = null

fun <R> myRun(block: () -> R): R {
    return block()
}

fun test(c: Inv<*>) {
    myRun {
        if (true) return@myRun // coerction to Unit

        c.invToInv()?.let {}
    }
}

fun box(): String {
    test(Inv<Int>())
    return "OK"
}