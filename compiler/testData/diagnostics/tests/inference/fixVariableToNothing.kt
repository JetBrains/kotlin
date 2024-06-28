// FIR_IDENTICAL
// completion order here: X, Y, WHEN_VARIABLE
fun <T> List<T>.optimizeReadOnlyList() = when (size) {
    0 -> emptyList() // here type variable Y will be fixed to Nothing
    1 -> listOf(this[0])
    else -> this
}
fun <X> listOf(element: X): List<X> = TODO()
fun <Y> emptyList(): List<Y> = TODO()


fun test(l: List<String>): List<String> {
    val foo = l.optimizeReadOnlyList()
    return foo
}
