fun foo(p: (Int, () -> Int) -> Unit) {
    p(1, <caret>{ 2 })
}
