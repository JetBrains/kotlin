// FIR_IDENTICAL
fun <T> funWithTypeParameterWithTwoUpperBounds(a: T, b: T)
        where T : Comparable<T>, T : Long = a + b
