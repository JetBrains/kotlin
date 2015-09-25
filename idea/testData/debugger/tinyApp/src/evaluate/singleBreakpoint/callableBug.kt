package callableBug

fun main(args: Array<String>) {
    val callable = 1
    arrayOf(1, 2).map {
       it + 1
       //Breakpoint!
    }.forEach { it + 2 }
}

// STEP_INTO: 1

// EXPRESSION: callable
// RESULT: 1: I

// EXPRESSION: it
// RESULT: 2: I