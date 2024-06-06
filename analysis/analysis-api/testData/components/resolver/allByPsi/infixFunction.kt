infix fun Int.myMinus(i: Int): Int = this - i

fun usage(parameter: Int) {
    val newParameter = parameter myMinus 5
    val oneMoreParameter = newParameter + 1 myMinus 4
    val explicitForm = newParameter.plus(parameter.myMinus(oneMoreParameter))
}
