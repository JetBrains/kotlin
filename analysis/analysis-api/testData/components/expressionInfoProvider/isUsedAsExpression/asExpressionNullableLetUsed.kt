fun test(v: Any?) {
    val x = (v as? String)?.<expr>let {
        it.length
    }</expr>
}