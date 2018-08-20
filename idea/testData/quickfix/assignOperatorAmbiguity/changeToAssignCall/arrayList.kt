// "Change to 'minusAssign()' call" "true"
// WITH_RUNTIME

fun test() {
    var list = ArrayList<Int>()
    list <caret>-= 2
}