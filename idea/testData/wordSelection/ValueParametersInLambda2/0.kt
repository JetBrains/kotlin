fun foo(f: (Int) -> Int) {}

fun test() {
    foo { it -> <caret>it + 1 }
}