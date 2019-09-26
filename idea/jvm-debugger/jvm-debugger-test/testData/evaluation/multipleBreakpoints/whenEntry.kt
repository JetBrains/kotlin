package whenEntry

fun main(args: Array<String>) {
    val a = 1

    // EXPRESSION: a
    // RESULT: 1: I
    val b = when {
        //Breakpoint!
        a == 1 -> 1 + 1
        else -> 1 + 2
    }

    // EXPRESSION: a
    // RESULT: 1: I
    val c = when {
        //Breakpoint!
        a == 1 -> { 1 + 1 }
        else -> 1 + 2
    }

    // EXPRESSION: a
    // RESULT: 1: I
    val d = when {
        //Breakpoint!
        a == 1 -> {
            1 + 1
        }
        else -> 1 + 2
    }
}