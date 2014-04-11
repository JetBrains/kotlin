// IS_APPLICABLE: FALSE
// ERROR: For-loop range must have an iterator() method
fun foo(bar: Map<String, String>) {
    for (a <caret>in bar) {

    }
}