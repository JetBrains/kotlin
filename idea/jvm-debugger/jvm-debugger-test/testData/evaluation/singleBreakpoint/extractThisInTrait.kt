package extractThisInTrait

fun main(args: Array<String>) {
    TImpl().foo()
}

interface T {
    fun foo() {
        //Breakpoint!
        val a = 1
    }

    val prop: Int
        get() = 1
}

class TImpl(): T

// EXPRESSION: prop
// RESULT: 1: I

// EXPRESSION: this.prop
// RESULT: 1: I