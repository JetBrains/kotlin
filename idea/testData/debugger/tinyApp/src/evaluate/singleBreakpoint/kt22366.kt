package kt22366

fun main(args: Array<String>) {
    val yy = (0 .. 10).map { a ->
        //Breakpoint! (lambdaOrdinal = -1)
        (0 .. 10).map { b -> b }.max()
    }
}

// EXPRESSION: a
// RESULT: 0: I

// EXPRESSION: b
// RESULT: Unresolved reference: b