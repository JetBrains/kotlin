fun foo(a: Int): Int {
    a.let {
        <expr>if (it > 0) return@foo it else return -it</expr>
    }
    return 0
}