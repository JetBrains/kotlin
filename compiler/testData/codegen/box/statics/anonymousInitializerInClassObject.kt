// IGNORE_BACKEND_FIR: JVM_IR
class Foo {
    companion object {
        val bar: String

        init {
            bar = "OK"
        }
    }
}

fun box(): String {
    return Foo.bar
}