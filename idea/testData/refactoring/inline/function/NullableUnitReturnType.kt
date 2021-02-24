fun <T> doIt(p: () -> T): T = p()
fun Any.doDo() = Unit

abstract class A {
    abstract fun a()
}

fun foo<caret>() {
    null?.doDo()
}

class B : A() {
    override fun a() = doIt {
        foo()
    }
}