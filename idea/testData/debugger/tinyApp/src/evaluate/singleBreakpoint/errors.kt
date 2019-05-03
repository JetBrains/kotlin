package errors

fun main(args: Array<String>) {
    MyClass().baseFun()
}

class MyClass: Base() {
    override fun baseFun() {
        var prop = 1
        var prop2 = 2

        //Breakpoint!
        prop.minus(prop2)
    }
}

open class Base {
    open fun baseFun() {}
}

// EXPRESSION: a
// RESULT: Unresolved reference: a

// EXPRESSION: a + 1
// RESULT: Unresolved reference: a

// EXPRESSION: prop.
// RESULT: Expecting an element; looking at ERROR_ELEMENT '(1,6) in /fragment.kt

// EXPRESSION: super.baseFun()
// RESULT: Evaluation of 'super' call expression is not supported