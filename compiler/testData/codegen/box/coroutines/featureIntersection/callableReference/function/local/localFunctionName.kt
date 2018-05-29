// IGNORE_BACKEND: JS

// COMMON_COROUTINES_TEST

fun box(): String {
    suspend fun OK() {}

    return ::OK.name
}
