// IGNORE_BACKEND_FIR: JVM_IR
open class A<T> {
    open fun f(args : Array<T>) {}
}

class B(): A<String>() {
    override fun f(args : Array<String>) {}
}

fun box(): String {
    B()
    return "OK"
}