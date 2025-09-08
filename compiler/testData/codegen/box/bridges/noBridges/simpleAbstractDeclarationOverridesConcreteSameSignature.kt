open class A {
    open fun foo(): String = "A"
}

abstract class B : A() {
    override abstract fun foo(): String
}

fun box(): String {
    return "OK"
}