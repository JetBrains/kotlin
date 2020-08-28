fun foo(f: (Int) -> Int) {}

fun test() {
    foo { it -> <caret><selection>it + 1</selection> }
}