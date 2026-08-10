// WITH_STDLIB

fun shadowing(x: Int) {
    fun g(x: Int) {
        return x
    }
    {x: Int -> x}
    x + 1
    return
}

fun selfReferential(x: Int) {
    val x = x
    val (x, y) = x to 1
    for (x in x..y) {
    }
}
