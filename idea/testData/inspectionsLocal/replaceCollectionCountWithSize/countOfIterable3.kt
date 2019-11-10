// PROBLEM: none
// WITH_RUNTIME

fun foo(iterable: Iterable<String>) {
    iterable.run {
        <caret>count()
    }
}
