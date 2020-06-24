// WITH_RUNTIME
const val ZERO = 0.0F

fun test(list: List<Float>) {
    list.fold<caret>(ZERO) { acc, i -> acc + i }
}