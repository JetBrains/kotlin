package inlineFunctionalExpression

fun main(args: Array<String>) {
    val a = array(1)
    // EXPRESSION: it
    // RESULT: Unresolved reference: it
    // STEP_INTO: 1
    //Breakpoint!
    a.map(fun (it): Int { return it * 1 })
}

