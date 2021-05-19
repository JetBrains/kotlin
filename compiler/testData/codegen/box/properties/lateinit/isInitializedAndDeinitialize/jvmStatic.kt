// WITH_RUNTIME
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
object Test {
    @JvmStatic
    lateinit var value: Any

    val isInitialized
        get() = Test::value.isInitialized

    val isInitializedThroughFn
        get() = self()::value.isInitialized

    fun self() = Test
}

fun box(): String {
    if (Test.isInitialized) return "fail 1"
    Test.value = "OK"
    if (!Test.isInitializedThroughFn) return "fail 2"
    return Test.value as String
}