// LANGUAGE: +JvmOptimizeImmediateRethrowForSuspend
// WITH_COROUTINES
// TREAT_AS_ONE_FILE

suspend fun foo() {}

// Generated: the first resume point is outside of try.
suspend fun bar(): Int {
    foo()
    try {
        foo()
    } catch (e: Exception) {
    }
    return 42
}

// Not generated: the only resume point is inside try.
suspend fun baz() {
    try {
        foo()
    } catch (e: Exception) {
    }
}

fun builder(c: suspend () -> Unit) {}

fun test() {
    // Generated.
    builder {
        foo()
        foo()
    }
    // Generated: state 0 and the last resume point are outside of try.
    builder {
        try {
            foo()
        } catch (e: Exception) {
        }
        foo()
    }
}

// 3 protected final synthetic wouldImmediatelyRethrowException\(\)Z
