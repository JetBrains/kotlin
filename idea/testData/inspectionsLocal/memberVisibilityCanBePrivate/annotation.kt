// WITH_RUNTIME
// SKIP_ERRORS_BEFORE
// SKIP_ERRORS_AFTER
// PROBLEM: none

annotation class Ann(val <caret>x: String) {
    fun foo() {
        println(x)
    }
}