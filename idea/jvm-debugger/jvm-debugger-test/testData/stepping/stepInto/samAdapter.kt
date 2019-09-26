package samAdapter

fun main(args: Array<String>) {
    //Breakpoint!
    val a = 1
    runReadAction { 1 }
}

fun runReadAction(action: () -> Int): Int {
    return forTests.MyJavaClass.runReadAction<Int>(action)
}

// STEP_INTO: 8