// IS_APPLICABLE: false
fun test(i: Int) {
    if (i == 1) println(1) else <caret>if (i == 2) println(2) else println(3)
}

fun println(i: Int) {}