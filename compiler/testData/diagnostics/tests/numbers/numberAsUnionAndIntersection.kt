// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

fun test(numbers: List<Number>) {

    // Short <: R, ILT <: R => CST(Short, ILT) = Short
    // => we should pick Short in such situation as it more specific
    shortExpected { getShort() ?: 1 }

    // Short <: R, ILT <: R, R <: Comparable<R> =>
    // Short <: Comparable<R>, ILT <: Comparable<R> =>
    // Comparable<Short> <: Comparable<R>, Comparable<ILT> <: Comparable<R> =>
    // R <: Short, R <: ILT =>
    // R := Short, R := ILT
    // => we should pick Short as it more specific
    shortComparable { getShort() ?: 1 }
}

fun <R> shortExpected(f: () -> R) {}
fun <R : Comparable<R>> shortComparable(f: () -> R) {}

fun getShort(): Short? = 1