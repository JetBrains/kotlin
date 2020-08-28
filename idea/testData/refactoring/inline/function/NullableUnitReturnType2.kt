fun <T> doIt(p: () -> T): T = p()
fun Any.doDo() = Unit

abstract class A {
    abstract fun a()
}

fun foo<caret>() {
    1.let { it.let { it.let { it.let { null?.doDo() } } } }
}

class B : A() {
    override fun a() = doIt {
        foo()
    }
}