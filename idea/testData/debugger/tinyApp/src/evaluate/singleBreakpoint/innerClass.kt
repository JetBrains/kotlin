package innerClass

fun main(args: Array<String>) {
    MyClass().MyInnerClass().innerFun()
}

class MyClass {
    inner class MyInnerClass {
        val innerProp = 1

        fun innerFun() {
            //Breakpoint!
            baseFun(innerProp)
        }
    }

    fun baseFun(i: Int) = i
}

// EXPRESSION: baseFun(innerProp)
// RESULT: 1: I