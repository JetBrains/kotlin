// IGNORE_BACKEND_FIR: JVM_IR
class Foo(private val s: String) {
    inner class Inner {
        private val x = {
            this@Foo.s
        }()
    }

    val f = Inner()

}

fun box(): String {
    Foo("!")
    return "OK"
}