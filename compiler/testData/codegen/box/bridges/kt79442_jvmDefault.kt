// JVM_DEFAULT_MODE: enable
// JVM_TARGET: 1.8
// WITH_STDLIB

interface Repository<T : Any> {
    suspend fun get(): Result<T>
    suspend fun local(): Result<T> = get()
    suspend fun remote(): Result<T> = get()
}

fun box(): String {
    return "OK"
}