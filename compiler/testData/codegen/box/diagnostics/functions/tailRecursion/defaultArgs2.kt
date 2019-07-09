// DONT_RUN_GENERATED_CODE: JS

tailrec fun test(x : Int = 0, e : Any = "a") {
    if (x < 100000 && !e.equals("a")) {
        throw IllegalArgumentException()
    }
    if (x > 0) {
        test(x - 1)
    }
}

fun box() : String {
    test(100000, "b")
    return "OK"
}