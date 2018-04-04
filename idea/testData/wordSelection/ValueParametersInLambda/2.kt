fun foo(f: (Int) -> Int) {}

fun test() {
    foo { <caret><selection>it -> it + 1</selection> }
}