package stepOverCatchClause

fun main(args: Array<String>) {
    try {
        bar()
    }
    catch(e: Exception) {
        val a = e
    }
}

fun bar() {
    //Breakpoint!
    throw IllegalStateException()
}

// STEP_OVER: 2