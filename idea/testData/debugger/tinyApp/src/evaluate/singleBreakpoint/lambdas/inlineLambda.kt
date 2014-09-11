package inlineLambda

fun main(args: Array<String>) {
    val a = array(1)
    // EXPRESSION: it
    // RESULT: Unresolved reference: it
    // STEP_INTO: 1
    //Breakpoint!
    a.map { it * 1 }
}

