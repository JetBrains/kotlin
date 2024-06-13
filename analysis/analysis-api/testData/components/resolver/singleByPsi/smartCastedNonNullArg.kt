private fun test(s: String?) {
    if (s != null) {
        <expr>foo(s)</expr>
    }
}

private fun foo(s: String) {
}
