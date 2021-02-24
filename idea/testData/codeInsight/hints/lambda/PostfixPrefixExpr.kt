// MODE: return
fun bar() {
    var test = 0
    run {
        test
        test++<# ^run #>
    }

    run {
        test
        ++test<# ^run #>
    }
}