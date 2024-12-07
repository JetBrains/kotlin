// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved
inline fun <T> withCatch(block: () -> T?) : T? {
    try {
        return block()
    } catch (e: NullPointerException) {
        return null
    } finally {
    }
}
fun f1() = withCatch<Int> { null }
fun f2() = withCatch<Unit> { null }
fun f3() = withCatch<Nothing> { null }

inline fun <T> withOutCatch(block: () -> T?) : T? {
    try {
        return block()
    } finally {
    }
}
fun f4() = withOutCatch<Int> { null }
fun f5() = withOutCatch<Unit> { null }
fun f6() = withOutCatch<Nothing> { null }


fun box() : String {
    if (f1() != null) return "FAIL1"
    if (f2() != null) return "FAIL2"
    if (f3() != null) return "FAIL3"
    if (f4() != null) return "FAIL4"
    if (f5() != null) return "FAIL5"
    if (f6() != null) return "FAIL6"
    return "OK"
}