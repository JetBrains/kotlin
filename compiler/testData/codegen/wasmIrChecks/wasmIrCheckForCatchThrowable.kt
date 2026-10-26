// TARGET_BACKEND: WASM

// A single `catch (e: Throwable)` should be kept as is by CatchMerger, but it gets a merged catch parameter
// WASM_CHECK_LOCAL_IN_FUNCTION: name=merged_catch_param inFunction=catchThrowable
// WASM_CHECK_LOCAL_IN_FUNCTION: name=e inFunction=catchThrowable

// Several catches are merged into one catching Throwable
// WASM_CHECK_LOCAL_IN_FUNCTION: name=merged_catch_param inFunction=multiCatch

fun catchThrowable(shouldThrow: Boolean): String {
    try {
        if (shouldThrow) throw RuntimeException()
        return "NO_EXCEPTION"
    } catch (e: Throwable) {
        return "CAUGHT"
    }
}

fun multiCatch(shouldThrow: Boolean): String {
    try {
        if (shouldThrow) throw IllegalStateException()
        return "NO_EXCEPTION"
    } catch (e: IllegalArgumentException) {
        return "IAE"
    } catch (e: IllegalStateException) {
        return "ISE"
    }
}

fun box(): String {
    if (catchThrowable(false) != "NO_EXCEPTION") return "fail 1"
    if (catchThrowable(true) != "CAUGHT") return "fail 2"
    if (multiCatch(false) != "NO_EXCEPTION") return "fail 3"
    if (multiCatch(true) != "ISE") return "fail 4"
    return "OK"
}
