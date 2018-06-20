// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

var result = "fail 2"
object Foo {
    @JvmStatic
    private val a = "OK"

    val b = { a }
    val c = Runnable { result = a }
}

fun box(): String {
    if (Foo.b() != "OK") return "fail 1"

    Foo.c.run()

    return result
}
