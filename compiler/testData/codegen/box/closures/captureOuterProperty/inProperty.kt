// IGNORE_BACKEND_FIR: JVM_IR
interface T {
    fun result(): String
}

class A(val x: String) {
    fun foo() = object : T {
        val bar = x

        override fun result() = bar
    }
}

fun box() = A("OK").foo().result()
