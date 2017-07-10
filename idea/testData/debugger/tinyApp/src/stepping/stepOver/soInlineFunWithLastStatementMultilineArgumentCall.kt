package soInlineFunWithLastStatementMultilineArgumentCall

fun main(args: Array<String>) {
    var k = 444
    bar {
        val b = 1
    }

    k++
}

inline fun bar(f: (Int) -> Unit) {
    //Breakpoint!
    f(1)
}

// STEP_OVER: 2