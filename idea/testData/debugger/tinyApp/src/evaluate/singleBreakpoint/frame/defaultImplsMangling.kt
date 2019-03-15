package defaultImplsMangling

interface IFoo {
    val x: Int

    fun foo() {
        //Breakpoint!
        val a = 5
    }
}

class Foo : IFoo {
    override val x: Int = 1
}

fun main() {
    Foo().foo()
}

// SHOW_KOTLIN_VARIABLES
// PRINT_FRAME

// EXPRESSION: x
// RESULT: 1: I