// IGNORE_BACKEND_WITHOUT_CHECK: JS

tailrec fun test(x : Int = 0, e : Any = "a") {
    if (!e.equals("a")) {
        throw IllegalArgumentException()
    }
    if (x > 0) {
        test(x - 1)
    }
}

fun box() : String {
    test(100000)
    return "OK"
}