// WITH_RUNTIME
fun <T> doIt(p: () -> T): T = p()
fun Any.doDo() = Unit

abstract class A {
    abstract fun a()
}

class B : A() {
    override fun a() = doIt {
        1.let { it.let { it.let { it.let { it.doDo() } } } }
        Unit<caret>
    }
}