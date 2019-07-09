// "Change type to MutableSet" "false"
// DISABLE-ERRORS
// ACTION: Replace overloaded operator with function call
// WITH_RUNTIME
fun main() {
    val set = setOf(1, 2, 3)
    set[1]<caret> = 10
}