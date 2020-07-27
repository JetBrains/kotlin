fun test(i: Int) {
    if<caret> (i == 1) {
        println(1)
    } else if (i == 2) {
        println(2)
    } else {
        println(3)
    }
}

fun println(i: Int) {}