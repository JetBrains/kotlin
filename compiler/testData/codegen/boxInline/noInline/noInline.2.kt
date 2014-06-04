inline fun calc(s: (Int) -> Int, noinline p: (Int) -> Int) : Int {
    val z = p
    return s(11) + z(11) + p(11)
}