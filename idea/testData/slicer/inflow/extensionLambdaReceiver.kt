// FLOW: IN
// RUNTIME_WITH_SOURCES

fun foo() {
    with("A") {
        val <caret>v = this
    }
}
