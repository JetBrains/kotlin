// PROBLEM: '+=' create new map under the hood
// FIX: Assign to local variable
// WITH_RUNTIME
fun test() {
    var map = mapOf(1 to 2)
    <caret>map += 3 to 4
}