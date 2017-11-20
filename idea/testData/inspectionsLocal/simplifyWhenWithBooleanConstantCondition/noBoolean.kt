// PROBLEM: none
// WITH_RUNTIME
fun test(i: Int) {
    <caret>when {
        i == 1 -> println(1)
        else -> println(2)
    }
}