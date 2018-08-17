// PROBLEM: '+=' create new set under the hood
// FIX: Assign to local variable
// WITH_RUNTIME
fun test2() {
    var set = setOf(1)
    <caret>set += 2
}