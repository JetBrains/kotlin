package lambdaOnReturn

// KT-14615

fun main(args: Array<String>) {
    val f1 = MyClass()
    f1(1)
}

fun MyClass(): (Int) -> String {
    // EXPRESSION: y
    // RESULT: 1: I
    //Breakpoint! (lambdaOrdinal = 1)
    return { y: Int -> y.toString() }
}
