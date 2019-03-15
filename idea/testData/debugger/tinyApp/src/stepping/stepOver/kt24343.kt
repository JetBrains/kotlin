package kt24343

fun main(args: Array<String>) {
    val list = listOf(1, 2)
    //Breakpoint!
    list.map { it + 1 }
    println()
}

// STEP_OVER: 1