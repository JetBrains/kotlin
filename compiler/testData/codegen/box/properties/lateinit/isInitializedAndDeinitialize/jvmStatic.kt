// WITH_STDLIB
// TARGET_BACKEND: JVM
object Test {
    @JvmStatic
    lateinit var value: String

    val isInitialized
        get() = Test::value.isInitialized

    val isInitializedThroughFn
        get() = self()::value.isInitialized

    fun self() = Test.apply { value = "OK" }
}

fun box(): String {
    if (Test.isInitialized) return "fail 1"
    if (!Test.isInitializedThroughFn) return "fail 2"
    return Test.value
}
