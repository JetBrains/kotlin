fun foo(f: (Int) -> Int) {}

fun test() {
    foo <selection>{ it ->
        it + 1

        <caret>it + 1
    }</selection>
}