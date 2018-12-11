// WITH_RUNTIME
// FIX: Replace with 'repeat()'

fun test(list: List<String>) {
    list.for<caret>Each {}
}