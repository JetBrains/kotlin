package invisibleDeclarations

fun main(args: Array<String>) {
    A()
}

class A() {
    private companion object {
        val test = 5
    }

    init {
        // EXPRESSION: test
        // RESULT: 5: I
        //Breakpoint!
        val a = 1
    }
}
