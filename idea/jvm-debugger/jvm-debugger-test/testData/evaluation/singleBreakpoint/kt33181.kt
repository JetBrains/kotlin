package kt33181

fun main() {
    Bar().foo()
}

open class Foo {
    open fun foo(): Int {
        return 5
    }
}

class Bar : Foo() {
    override fun foo(): Int {
        //Breakpoint!
        return 6
    }
}

// EXPRESSION: super.foo()
// RESULT: 5: I

// EXPRESSION: for (i in 1..1) { super.foo()}
// RESULT: VOID_VALUE

// EXPRESSION: { super.foo() }
// RESULT: Evaluation of 'super' calls inside lambdas and functions is not supported

// EXPRESSION: fun() { super.foo() }
// RESULT: Evaluation of 'super' calls inside lambdas and functions is not supported

// EXPRESSION: if(true) { fun named() { super.foo() } } // hack to avoid "Anonymous functions with names are prohibited"
// RESULT: Evaluation of 'super' calls inside lambdas and functions is not supported