// !LANGUAGE: +ReleaseCoroutines
// IGNORE_BACKEND: JVM_IR

suspend fun produce(): Int = 1000

// 0 valueOf
// 1 boxInt
