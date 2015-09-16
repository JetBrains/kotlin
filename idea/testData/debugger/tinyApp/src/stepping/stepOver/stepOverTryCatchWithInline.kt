package stepOverTryCatchWithInline

fun main(args: Array<String>) {
    try {
        bar()
    }
    catch(e: Exception) {
        val a = 1
    }
}

fun bar() {
    //Breakpoint!
    val prop = 1
    // Try
    try {
        foo { test(1) }
    }
    catch(e: Exception) {
        foo { test(1) }
    }

    // Many catch clauses
    try {
        throw IllegalStateException()
    }
    catch(e: IllegalStateException) {
        foo { test(1) }
    }
    catch(e: Exception) {
        foo { test(1) }
    }

    // exception in lambda
    try {
        foo { throw IllegalStateException() }
    }
    catch(e: Exception) {
        foo { test(1) }
    }

    // Exception without catch
    foo { throw IllegalStateException() }
    val prop2 = 1
}

inline fun foo(f: () -> Int): Int {
    val a = 1
    return f()
}

fun test(i: Int) = 1

// STEP_OVER: 30