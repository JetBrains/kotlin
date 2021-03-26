// PROBLEM: none
// WITH_RUNTIME
fun test() {
    var map = mutableMapOf(1 to 2)
    map <caret>+= 3 to 4
}