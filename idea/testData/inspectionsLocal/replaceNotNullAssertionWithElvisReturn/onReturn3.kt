fun test(a: Int?): Int? {
    return check(a!!<caret>)
}

fun check(i: Int): Int = i