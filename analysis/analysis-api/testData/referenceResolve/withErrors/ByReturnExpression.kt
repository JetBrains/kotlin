// UNRESOLVED_REFERENCE
fun bar(block: () -> Unit) {}

fun foo() {
    bar {
        return@<caret>b
    }
}