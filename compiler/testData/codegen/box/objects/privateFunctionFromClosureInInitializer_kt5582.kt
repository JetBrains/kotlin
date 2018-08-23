// IGNORE_BACKEND: JVM_IR
interface T

object Foo {
    private fun foo(p: T) = p

    private val v: Int = {
        val x = foo(O)
        42
    }()

    private object O : T

    val result = v
}

fun box(): String {
    val foo = Foo
    if (foo.result != 42) return "Fail: ${foo.result}"
    return "OK"
}
