// PROBLEM: '+=' create new list under the hood
// FIX: Assign to local variable
// WITH_RUNTIME
fun test() {
    var list = listOf(1)
    <caret>list += 2
}