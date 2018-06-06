fun foo(f: (Int) -> String) {}

fun test() {
    foo(<caret>{ "" })
}