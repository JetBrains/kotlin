// WITH_RUNTIME
// FIX: Replace with 'repeat(size)'

fun test(list: List<String>) {
    list.for<caret>Each {}
}