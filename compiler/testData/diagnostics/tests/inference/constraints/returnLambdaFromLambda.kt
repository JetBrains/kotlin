// DIAGNOSTICS: -UNUSED_VARIABLE

fun testLambda() {
    val basicTest: (Int) -> Int = myRun {
        val x: Any? = null
        if (x is String) return@myRun { it -> <!DEBUG_INFO_SMARTCAST!>x<!>.length + it }
        if (x !is Int) return@myRun { it -> it }

        { it -> <!DEBUG_INFO_SMARTCAST!>x<!> + it }
    }

    val twoLambda: (Int) -> Int = myRun {
        val x: Int = 1
        run {
            val y: Int = 2
            { x + y }
        }
    }

}

inline fun <R> myRun(block: () -> R): R = block()