// FIX: Change type to mutable
// WITH_RUNTIME
fun test() {
    var map = mapOf(1 to 2)
    map += 3 to 4<caret>
}