abstract class A {
    abstract protected fun foo(): String
    abstract protected val bar: String
}

interface B {
    fun foo(): String
    val bar: String
}

fun test(x: A) {
    if (x is B) {
        x.foo()
        x.bar
    }
}
