package privateMember

fun main(args: Array<String>) {
    val base = Base()
    val derived = Derived()
    val derivedAsBase: Base = Derived()

    //Breakpoint!
    args.size
}

class MyClass {
    private fun privateFun() = 1
    private val privateVal = 1

    private class PrivateClass {
        val a = 1
    }
}

open class Base {
    private fun privateFun() = 2
}

class Derived: Base() {
    private fun privateFun() = 3
}

// EXPRESSION: MyClass().privateFun()
// RESULT: 1: I

// EXPRESSION: MyClass().privateVal
// RESULT: 1: I

// EXPRESSION: MyClass.PrivateClass().a
// RESULT: 1: I

// EXPRESSION: base.privateFun()
// RESULT: 2: I

// EXPRESSION: derived.privateFun()
// RESULT: 3: I

// EXPRESSION: derivedAsBase.privateFun()
// RESULT: 2: I
