// !LANGUAGE: -ExtendedMainConvention -ReleaseCoroutines
// Does not run on JVM_IR since it uses experimental coroutines
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

// uses kotlin.coroutines.experimental classes under the hood

suspend fun main(args: Array<String>) {}
