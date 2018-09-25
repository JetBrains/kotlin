// FIX: none
// WITH_RUNTIME
fun test() {
    listOf(1).forEach {
        if (it == 10) <caret>return
    }
}
