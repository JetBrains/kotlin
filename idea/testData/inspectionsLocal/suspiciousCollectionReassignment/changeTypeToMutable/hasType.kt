// FIX: Change type to mutable
// WITH_RUNTIME
fun test() {
    var list: List<Int> = listOf(1)
    list += 2<caret>
}