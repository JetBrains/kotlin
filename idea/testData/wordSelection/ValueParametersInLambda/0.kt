fun foo(f: (Int) -> Int) {}

fun test() {
    foo { <caret>it -> it + 1 }
}