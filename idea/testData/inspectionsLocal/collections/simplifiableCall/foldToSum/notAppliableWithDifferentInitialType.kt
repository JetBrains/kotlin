// PROBLEM: none
// WITH_RUNTIME
fun test(list: List<Int>) {
    val sum: Double = list.fold<caret>(0.0) { acc, i -> acc + i }
}