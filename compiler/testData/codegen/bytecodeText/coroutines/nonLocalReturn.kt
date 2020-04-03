// WITH_COROUTINES
// TREAT_AS_ONE_FILE

inline suspend fun runReturning(lambda: suspend () -> Unit): Unit =
    lambda()

inline suspend fun myRun(lambda: suspend () -> String): String {
    runReturning { return lambda() }
    return "fail: did not return from lambda"
}

// 0 NON_LOCAL_RETURN