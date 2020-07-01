// WITH_RUNTIME
fun foo() = listOf(1)

fun test(list: List<Int>): List<Int> {
    return if (list.isEmpty<caret>()) {
        println()
        foo()
    } else {
        list
    }
}