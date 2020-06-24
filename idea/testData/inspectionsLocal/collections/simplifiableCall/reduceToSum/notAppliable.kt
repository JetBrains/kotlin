// PROBLEM: none
// WITH_RUNTIME
fun test(list: List<Int>) {
    list.reduce<caret> { acc, i -> acc + 1 }
}