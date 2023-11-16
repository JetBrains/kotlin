// DONT_TARGET_EXACT_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: JS

object Test

fun test1(): String {
    val a = "test 1: " + Test

    val test = Test
    val b = "test 1: " + test

    if (a != b) return "<!EVALUATED("Fail 1: "")!>Fail 1: \"<!>$a<!EVALUATED("" != "")!>\" != \"<!>$b<!EVALUATED(""")!>\"<!>"
    return "OK"
}

fun test2(): String {
    val a = "test 2: " + Test.toString()

    val test = Test
    val b = "test 2: " + test.toString()

    if (a != b) return "<!EVALUATED("Fail 2: "")!>Fail 2: \"<!>$a<!EVALUATED("" != "")!>\" != \"<!>$b<!EVALUATED(""")!>\"<!>"
    return "OK"
}

fun test3(): String {
    val a = "<!EVALUATED("test 3: ")!>test 3: <!>$Test"

    val test = Test
    val b = "<!EVALUATED("test 3: ")!>test 3: <!>$test"

    if (a != b) return "<!EVALUATED("Fail 3: "")!>Fail 3: \"<!>$a<!EVALUATED("" != "")!>\" != \"<!>$b<!EVALUATED(""")!>\"<!>"
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
