val x = myRun {
    val inLambda = 10
    println(inLambda)
    inLambda
}

fun println(any: Any) {

}

inline fun <R> myRun(block: () -> R): R {
    return block()
}
