fun test() {
    var x: Int? = 42
    run {
        x = null
    }
    run {
        if (x != null)
            <expr>x.inc()</expr>
    }
}