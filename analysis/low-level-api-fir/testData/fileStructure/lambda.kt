val x = myRun {
    val inLambda = 10
    println(inLambda)
    inLambda
}/* DeclarationStructureElement */

fun println(any: Any) {/* DeclarationStructureElement */

}

inline fun <R> myRun(block: () -> R): R {/* DeclarationStructureElement */
    return block()
}
