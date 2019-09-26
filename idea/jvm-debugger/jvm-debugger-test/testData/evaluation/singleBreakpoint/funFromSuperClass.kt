package funFromSuperClass

fun main(args: Array<String>) {
    Derived().test()
}

open class Base {
    fun foo() = 1

    fun test() {
        //Breakpoint!
        val a = 1
    }
}

class Derived: Base()


// EXPRESSION: foo()
// RESULT: 1: I