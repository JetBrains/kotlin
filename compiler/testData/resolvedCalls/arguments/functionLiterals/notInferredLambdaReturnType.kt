fun <T> foo(f: () -> T) {}

fun test() {
    <caret>foo { b }
}