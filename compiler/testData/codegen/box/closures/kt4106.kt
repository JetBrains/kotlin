fun <T> eval(fn: () -> T) = fn()

class Foo(private val s: String) {
    inner class Inner {
        private val x = eval {
            this@Foo.s
        }
    }

    val f = Inner()

}

fun box(): String {
    Foo("!")
    return "OK"
}