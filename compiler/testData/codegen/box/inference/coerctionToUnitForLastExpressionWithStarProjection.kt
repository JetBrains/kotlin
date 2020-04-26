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

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: TODO