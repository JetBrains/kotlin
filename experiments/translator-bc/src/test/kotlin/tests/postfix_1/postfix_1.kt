fun postfix_1_plus_before(x: Int): Int {
    var xx = x
    val z = xx++
    return z
}

fun postfix_1_plus_after(x: Int): Int {
    var xx = x
    val z = xx++
    return xx
}

fun postfix_1_plus_inside(x: Int): Int {
    var xx = x
    xx++
    return xx
}

fun postfix_1_minus_before(x: Int): Int {
    var xx = x
    val z = xx--
    return z
}

fun postfix_1_minus_after(x: Int): Int {
    var xx = x
    val z = xx--
    return xx
}

fun postfix_1_minus_inside(x: Int): Int {
    var xx = x
    xx--
    return xx
}