fun foo(f: (Int) -> String) {}

fun test() {
    <caret>foo { x -> "$x"}
}