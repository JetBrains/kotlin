// See KT-15334: incorrect reassignment in do...while

fun test() {
    do {
        val s: String
        s = ""
    } while (s == "")
}

fun test2() {
    while (true) {
        val s: String
        s = ""
        if (s != "") break
    }
}
