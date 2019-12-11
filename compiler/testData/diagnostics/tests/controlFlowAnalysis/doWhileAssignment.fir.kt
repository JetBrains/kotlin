// See KT-15334: incorrect reassignment in do...while

fun test() {
    do {
        val s: String
        s = ""
    } while (s == "")
}

fun test2() {
    do {
        val s: String
        s = "1"
        s = s + "2"
    } while (s == "1")
}

fun test3() {
    val s: String
    do {
        s = ""
    } while (s != "")
}
