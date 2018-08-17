// FIX: Assign to local variable
// WITH_RUNTIME
fun test() {
    var set = setOf(1)
    <caret>set -= 1
}