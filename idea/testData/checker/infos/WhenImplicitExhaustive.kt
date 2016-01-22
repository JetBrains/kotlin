fun foo(b: Boolean): Int {
    <info descr="When is implicitly exhaustive">when</info> (b) {
        true -> return 1
        false -> return 0
    }
}
