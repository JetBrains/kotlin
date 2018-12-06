// WITH_RUNTIME
// FIX: Replace with 'repeat(size)'

fun test(sequence: Sequence<String>) {
    sequence.for<caret>Each {}
}