// PROBLEM: none
// WITH_RUNTIME
fun test(list: List<Int>) {
    list.reduce<caret> { acc, i -> 2 + acc }
}