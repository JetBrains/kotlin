fun <T> foo(f: (T) -> String) {}

fun test() {
    <caret>foo { x -> "$x"}
}