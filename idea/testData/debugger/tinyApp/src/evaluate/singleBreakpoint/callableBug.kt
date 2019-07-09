package callableBug

fun main(args: Array<String>) {
    val callable = 1
    arrayOf(1, 2).map {
       it + 1
        //Breakpoint! (lambdaOrdinal = 1)
    }.forEach { it + 2 }
}

// EXPRESSION: callable
// RESULT: 1: I

// EXPRESSION: it
// RESULT: 2: I