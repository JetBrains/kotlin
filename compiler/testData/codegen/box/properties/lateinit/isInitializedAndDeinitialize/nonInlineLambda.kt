// LANGUAGE: -NativeJsProhibitLateinitIsInitializedIntrinsicWithoutPrivateAccess
// WITH_STDLIB

fun <T> eval(fn: () -> T) = fn()

class Foo {
    private lateinit var foo: String

    fun test(): Boolean {
        val result = eval { ::foo.isInitialized }
        foo = ""
        return result
    }
}

fun box(): String {
    val f = Foo()
    if (f.test()) return "Fail 1"
    if (!f.test()) return "Fail 2"
    return "OK"
}
