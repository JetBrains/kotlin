fun foo(f: (Int) -> Int) {}

fun test() {
    foo { it ->
        it + 1

        <caret>it + 1
    }
}