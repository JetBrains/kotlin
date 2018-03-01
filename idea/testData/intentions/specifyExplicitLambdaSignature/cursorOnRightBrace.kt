fun foo(f: (String) -> Int) {}
fun test() {
    foo { it.length <caret>}
}
