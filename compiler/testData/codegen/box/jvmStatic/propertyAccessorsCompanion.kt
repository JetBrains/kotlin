// WITH_RUNTIME

var result = "fail 2"
class Foo {
    val b = { a }
    val c = Runnable { result = a }

    companion object {
        @JvmStatic
        private val a = "OK"
    }
}

fun box(): String {
    if (Foo().b() != "OK") return "fail 1"

    Foo().c.run()

    return result
}
