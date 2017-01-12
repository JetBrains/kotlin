// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

var result = "fail 1"
object Foo {
    @JvmStatic
    private val a = "OK"

    fun foo() = run { result = a }
}

fun box(): String {
    Foo.foo()

    return result
}
