fun foo(a: Int): Int {
    a.let {
        <expr>if (it > 0) return it else return@foo -it</expr>
    }
    return 0
}