// "Replace with 'minusAssign()' call" "true"
// WITH_RUNTIME

fun test() {
    var set = HashSet<Int>()
    set <caret>-= 2
}