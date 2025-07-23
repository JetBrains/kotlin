// TARGET_BACKEND: JVM
// JVM_DEFAULT_MODE: enable
// JVM_TARGET: 1.8
// WITH_STDLIB

// CHECK_BYTECODE_TEXT

// FILE: Repository.kt
interface Repository<T : Any> {
    suspend fun get(): Result<T>
    suspend fun local(): Result<T> = get()
    suspend fun remote(): Result<T> = get()
}

// FILE: main.kt
fun box(): String {
    return "OK"
}

// @Repository$DefaultImpls.class:
// 0 @Lkotlin/coroutines/jvm/internal/DebugMetadata;
// 0 L$0