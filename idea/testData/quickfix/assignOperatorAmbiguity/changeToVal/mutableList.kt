// "Change 'list' to val" "true"
// WITH_RUNTIME

fun test() {
    var list = mutableListOf(1)
    list <caret>+= 2
}