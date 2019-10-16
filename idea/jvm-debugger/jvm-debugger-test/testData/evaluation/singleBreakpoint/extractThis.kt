package extractThis

fun main(args: Array<String>) {
    MyClass().test()
}

class MyClass {
    val prop = 1

    fun test() {
        val a = 1
        //Breakpoint!
        val b = 1
    }
}

// EXPRESSION: prop
// RESULT: 1: I

// EXPRESSION: this.prop
// RESULT: 1: I

// EXPRESSION: prop + a
// RESULT: 2: I