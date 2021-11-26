// CHECK_STRING_LITERAL_COUNT: function=foo count=1
fun foo(x: Int) = "foo $x"

// CHECK_STRING_LITERAL_COUNT: function=bar count=2 IGNORED_BACKENDS=JS
fun bar(x: Int) = "$x bar"

// CHECK_STRING_LITERAL_COUNT: function=baz count=1
fun baz(x: Int) = "${x.toString()} baz"

// CHECK_STRING_LITERAL_COUNT: function=beer count=2 IGNORED_BACKENDS=JS
fun beer(x: Int?) = "$x beer"

// CHECK_STRING_LITERAL_COUNT: function=quux count=2 IGNORED_BACKENDS=JS
fun quux(x: Int?) = "${x?.toString()} quux"

// CHECK_STRING_LITERAL_COUNT: function=test count=2
fun test(p: String?): String {
    return "${p ?: "Default"} test"
}

fun box(): String {
    if (test(null) != "Default test") return "fail 1: ${test(null)}"
    if (test("Good") != "Good test") return "fail 2: ${test("Good")}"
    if (foo(3) != "foo 3") return "fail 3: ${foo(3)}"
    if (bar(4) != "4 bar") return "fail 4: ${bar(4)}"
    if (baz(5) != "5 baz") return "fail 5: ${baz(5)}"
    if (beer(6) != "6 beer") return "fail 6: ${beer(6)}"
    if (beer(null) != "null beer") return "fail 7: ${beer(null)}"
    if (quux(8) != "8 quux") return "fail 8: ${quux(8)}"
    if (quux(null) != "null quux") return "fail 9: ${quux(null)}"

    return "OK"
}