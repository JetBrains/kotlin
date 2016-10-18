package noParameterLambdaArgumentCallInLambda

fun main(args: Array<String>) {
    //Breakpoint! (lambdaOrdinal = 1)
    foo { val a = 11 } // Breakpoint inside lambda argument
}

inline fun foo(s: () -> Unit) {
    val x = 22
    s()
    val y = 33
}