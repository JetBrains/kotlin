fun test(b: String?) {
    b?.let {
        <expr>return@let 5</expr>
    }
}