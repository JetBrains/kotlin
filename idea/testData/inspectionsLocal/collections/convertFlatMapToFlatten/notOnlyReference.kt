// PROBLEM: none
// WITH_RUNTIME
fun test() {
    listOf(listOf(1)).flatMap<caret> { it + 1 }
}