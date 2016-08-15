fun foo(a: Int) {
    when (a) {
        1 -> foo(a)<caret>
    }
}