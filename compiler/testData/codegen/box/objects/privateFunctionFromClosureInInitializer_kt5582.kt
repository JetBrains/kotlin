interface T

fun <T> eval(fn: () -> T) = fn()

object Foo {
    private fun foo(p: T) = p

    private val v: Int = eval {
        val x = foo(O)
        42
    }

    private object O : T

    val result = v
}

fun box(): String {
    val foo = Foo
    if (foo.result != 42) return "Fail: ${foo.result}"
    return "OK"
}
