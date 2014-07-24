import test.*

fun test1(s: Long): Boolean {
    return doSmth(s)
}

fun test2(s: Int): Boolean {
    return doSmth(s)
}

inline fun <T> test3(s: T): Boolean {
    return doSmth(s)
}

fun box(): String {
    if (!test1(11111.toLong())) return "fail 1"
    if (!test2(11111)) return "fail 2"
    if (!test3(11111)) return "fail 3.1"
    if (!test3("11111")) return "fail 3.2"
    if (!test3(11111.3)) return "fail 3.3"

    return "OK"
}