expect open class A() {
    open fun c(a: Int, b: String)
}

class C : A() {
    override fun c(a: Int, b: String) {}
}

public inline fun <T, R> with(receiver: T, block: T.() -> R): R {
    return receiver.block()
}