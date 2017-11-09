// WITH_RUNTIME
// ERROR: Body is not allowed for annotation class
// PROBLEM: none

annotation class Ann(val <caret>x: String) {
    fun foo() {
        println(x)
    }
}