// COMMON_COROUTINES_TEST
// WITH_RUNTIME

suspend fun catchException(): String {
    try {
        return suspendWithException()
    }
    catch(e: Exception) {
        return e.message!!
    }
}

suspend fun suspendWithException(): String = TODO()
