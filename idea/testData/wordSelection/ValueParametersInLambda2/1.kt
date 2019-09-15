fun foo(f: (Int) -> Int) {}

fun test() {
    foo { it -> <caret><selection>it</selection> + 1 }
}