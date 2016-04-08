package stepOverNonLocalReturnInLambda

fun main(args: Array<String>) {
    try {
        test1()
        test2()
        test3()
    }
    catch(e: Exception) {
        val a = 1
    }

    val c = 1
}

fun test1() {
    // STEP_OVER: 2
    // RESUME: 1
    //Breakpoint!
    val a = "aaa"
    synchronized(a) {
        if (a == "bbb") {
            return
        }
    }

    val c = 1
}

fun test2() {
    // STEP_OVER: 2
    // RESUME: 1
    //Breakpoint!
    val a = "aaa"
    synchronized(a) {
        if (a == "aaa") {
            return
        }
    }

    val c = 1
}

private fun test3() {
    inlineFunThrowException()
}

inline fun inlineFunThrowException() {
    // STEP_OVER: 2
    // RESUME: 1
    //Breakpoint!
    val a = 1
    synchronized(a) {
        throw IllegalArgumentException()
    }
}