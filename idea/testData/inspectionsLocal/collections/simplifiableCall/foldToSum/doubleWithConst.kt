// WITH_RUNTIME
const val ZERO = 0.0

fun test(list: List<Double>) {
    list.fold<caret>(ZERO) { acc, i -> acc + i }
}