fun useCallable(tag: String, callable: Callable<*>) {

}

fun main(args: Array<String>) {
    useCallable("A", Callable { println("Hello world") })

    useCallable(
            "B",
            Callable {
                println("Hello world")
            },
    )

    useCallable(
            "C",
            object : Callable<Unit> {
                override fun call() {
                    println("Hello world")
                }
            },
    )

    useCallable(
            "B",
            fun() {
                println("Hello world")
            },
    )
}

// SET_TRUE: CALL_PARAMETERS_LPAREN_ON_NEXT_LINE
// SET_TRUE: CALL_PARAMETERS_RPAREN_ON_NEXT_LINE
