// PROBLEM: none
// WITH_RUNTIME
fun test() {
    listOf(1).forEach {
        fun foo() {
            if (it == 10) <caret>return
        }
    }
}
