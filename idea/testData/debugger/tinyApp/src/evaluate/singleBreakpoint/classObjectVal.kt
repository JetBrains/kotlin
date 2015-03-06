package classObjectVal

fun main(args: Array<String>) {
    MyClass().test()
}

class MyClass {
    fun test() {
        //Breakpoint!
        val a = 1
    }

    default object {
        val coProp = 1
    }
}

// EXPRESSION: coProp
// RESULT: 1: I

// EXPRESSION: MyClass.coProp
// RESULT: 1: I