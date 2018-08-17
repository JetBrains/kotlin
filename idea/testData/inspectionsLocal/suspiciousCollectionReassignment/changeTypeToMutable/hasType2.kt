// FIX: Change type to mutable
// WITH_RUNTIME
fun test() {
    var list = listOf<Int>(1)
    list += 2<caret>
}