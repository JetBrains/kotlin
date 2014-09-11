package callableBug

fun main(args: Array<String>) {
    val callable = 1
    array(1, 2).map {
       it + 1
       //Breakpoint!
    }.forEach { it + 2 }
}

// EXPRESSION: callable
// RESULT: 1: I