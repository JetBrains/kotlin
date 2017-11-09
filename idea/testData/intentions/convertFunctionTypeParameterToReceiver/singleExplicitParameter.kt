fun foo(n: Int, action: (<caret>Int) -> Int) = action(n)

fun test() {
    foo(1) { n -> n + 1 }
}