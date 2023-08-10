// FIR_IDENTICAL
external open class A {
    open fun f(x: Int): Unit

    <!JS_NAME_CLASH!>open fun f(x: String): Unit<!>
}

class InheritClass : A() {
    <!JS_NAME_CLASH!>override fun f(x: Int): Unit<!> { }
}