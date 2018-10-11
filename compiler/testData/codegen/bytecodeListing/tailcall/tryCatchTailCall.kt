// COMMON_COROUTINES_TEST
suspend fun catchException(): String {
    try {
        return suspendWithException()
    }
    catch(e: Exception) {
        return e.message!!
    }
}

suspend fun suspendWithException(): String = TODO()
