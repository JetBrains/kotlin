// IS_APPLICABLE: false
fun foo(f: () -> Unit) {}

fun bar() {
    <caret>foo {}
}