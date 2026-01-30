object Test

fun test1(): String {
    val a = "test 1: " + Test

    val test = Test
    val b = "test 1: " + test

    if (a != b) return "Fail 1: \"$a\" != \"$b\""
    return "OK"
}

fun test2(): String {
    val a = "test 2: " + Test.toString()

    val test = Test
    val b = "test 2: " + test.toString()

    if (a != b) return "Fail 2: \"$a\" != \"$b\""
    return "OK"
}

fun test3(): String {
    val a = "test 3: $Test"

    val test = Test
    val b = "test 3: $test"

    if (a != b) return "Fail 3: \"$a\" != \"$b\""
    return "OK"
}

fun box(): String {
    val test1Result = test1()
    if (test1Result != "OK") return test1Result

    val test2Result = test2()
    if (test2Result != "OK") return test2Result

    val test3Result = test2()
    if (test3Result != "OK") return test3Result

    return "OK"
}
