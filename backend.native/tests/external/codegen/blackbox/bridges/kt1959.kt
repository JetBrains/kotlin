open class A<T> {
    open fun f(args : Array<T>) {}
}

class B(): A<String>() {
    override fun f(args : Array<String>) {}
}

fun foo(args: Array<String>) {
    B()
}

fun box(): String = "OK"
