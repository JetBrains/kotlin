// IGNORE_BACKEND_FIR: JVM_IR
interface Tr<T> {
    val prop: T
}

class A(a: Tr<Int>) : Tr<Int> by a

fun eat(x: Int) {}

fun box(): String {
    eat(A(object : Tr<Int> {
        override val prop = 42
    }).prop)
    return "OK"
}
