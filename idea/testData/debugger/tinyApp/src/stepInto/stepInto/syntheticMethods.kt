package syntheticMethods

fun main(args: Array<String>) {
    val d: Base<String> = Derived()
    //Breakpoint!
    d.foo("")
    val a = 1
}

open class Base<T> {
    open fun foo(t: T) {
        val a = 1
    }
}

class Derived: Base<String>() {
    override fun foo(t: String) {
        val a = 1
    }
}

// STEP_INTO: 3