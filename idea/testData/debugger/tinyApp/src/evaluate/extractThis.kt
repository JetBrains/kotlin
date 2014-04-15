package extractThis

fun main(args: Array<String>) {
    MyClass().test()
}

class MyClass {
    val prop = 1

    fun test() {
        //Breakpoint!
        val a = 1
    }
}

// EXPRESSION: prop
// RESULT: 1: I

// EXPRESSION: this.prop
// RESULT: 1: I