// IGNORE_BACKEND_FIR: JVM_IR
interface T {
    fun result(): String
}

open class B(val x: String)

class A : B("OK") {
    fun foo() = object : T {
        val bar = x

        override fun result() = bar
    }
}

fun box() = A().foo().result()
