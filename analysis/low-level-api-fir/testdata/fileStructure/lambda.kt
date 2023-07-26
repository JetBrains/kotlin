val x = myRun {
    val inLambda = 10
    println(inLambda)
    inLambda
}/* NonReanalyzableNonClassDeclarationStructureElement */

fun println(any: Any) {/* ReanalyzableFunctionStructureElement */

}

inline fun <R> myRun(block: () -> R): R {/* ReanalyzableFunctionStructureElement */
    return block()
}
