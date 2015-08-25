package inlineFunctionalExpression

fun main(args: Array<String>) {
    val a = array(1)
    // EXPRESSION: it
    // RESULT: 1: I
    // RESUME: 1
    //Breakpoint!
    a.map(fun (it): Int { return it * 1 })
}

