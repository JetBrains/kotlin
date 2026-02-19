inline fun calculate(): Int {
    val outer = {
        val inner = {
            40 + 2
        }
        inner()
    }
    return outer()
}
