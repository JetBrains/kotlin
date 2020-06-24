// PROBLEM: none
// WITH_RUNTIME
fun test(list: List<Int>) {
    list.fold<caret>(1) { acc, i -> acc + i }
}