// IGNORE_BACKEND_FIR: JVM_IR
interface T {
    fun result(): String
}

abstract class A<Z>(val x: Z)

open class B : A<String>("OK")

class C : B() {
    fun foo() = object : T {
        val bar = x

        override fun result() = bar
    }
}

fun box() = C().foo().result()
