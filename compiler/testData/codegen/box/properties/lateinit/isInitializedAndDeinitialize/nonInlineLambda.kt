// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

class Foo {
    private lateinit var foo: String

    fun test(): Boolean {
        val result = { ::foo.isInitialized }()
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
