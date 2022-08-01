fun test(b: String?) {
    b?.let {
        return@let <expr>5</expr>
    }
}