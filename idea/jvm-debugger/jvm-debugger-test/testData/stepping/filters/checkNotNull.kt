package checkNotNull

import forTests.MyJavaClass

fun main(args: Array<String>) {
    val myClass = MyJavaClass()
    //Breakpoint!
    val a: String = myClass.testNotNullFun()
    val b = 1
}

// STEP_INTO: 3
