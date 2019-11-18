// IGNORE_BACKEND_FIR: JVM_IR
interface T {
    fun result(): String
}

class A(val x: String) {
    fun getx() = x

    fun foo() = object : T {
        val bar = getx()

        override fun result() = bar
    }
}

fun box() = A("OK").foo().result()
