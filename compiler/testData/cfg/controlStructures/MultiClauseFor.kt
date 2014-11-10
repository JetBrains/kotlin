fun t1() {
    for (i in 1..2, j in 1..i) {
        doSmth(i + j)
    }
}

fun doSmth(i: Int) {}
