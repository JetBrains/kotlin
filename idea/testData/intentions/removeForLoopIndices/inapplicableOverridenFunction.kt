// WITH_RUNTIME
// IS_APPLICABLE: FALSE
fun Int.withIndex(): List<Pair<Int, Int>> = listOf<Pair<Int, Int>>()

fun foo(s: Int) {
    for ((index<caret>, a) in s.withIndex()) {

    }
}