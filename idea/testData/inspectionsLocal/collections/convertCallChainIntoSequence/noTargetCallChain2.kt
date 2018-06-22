// PROBLEM: none
// WITH_RUNTIME

fun test(list: List<Int>): List<Int> {
    return <caret>list.filter { it > 2 }.dropLast(1).filter { it > 2 }
}