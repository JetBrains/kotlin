fun foo(vararg x: String) {}

fun main() {
    val array = Array(5) { "$it" }
    foo(*array, *ar<caret>ray, *array)
}