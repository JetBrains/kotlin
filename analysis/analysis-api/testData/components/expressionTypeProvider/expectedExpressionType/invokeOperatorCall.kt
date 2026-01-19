fun test() {
    val func: (Int) -> String = { it.toString() }
    func(ar<caret>g)
}
