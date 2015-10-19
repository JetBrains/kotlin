fun bar(f: Int.() -> Unit, i: Int) {
    with (i) {
        f<caret>()
    }
}
