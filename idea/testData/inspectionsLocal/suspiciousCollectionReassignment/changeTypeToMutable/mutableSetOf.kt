// FIX: Change type to mutable
// WITH_RUNTIME
fun test() {
    var set = setOf(1)
    set += 1<caret>
}