// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
// CHECK_CASES_COUNT: function=foo count=4
// CHECK_IF_COUNT: function=foo count=1

var log = ""

fun foo(x: Int): String {
    log += "foo($x);"
    return when (x) {
        1 -> "one"
        2 -> "two"
        three(x) -> "three"
        4 -> "four"
        5 -> "five"
        else -> "many"
    }
}

fun three(x: Int): Int {
    log += "three($x);"
    return 3
}

fun box(): String {
    var result = (1..7).map(::foo).joinToString()

    if (result != "one, two, three, four, five, many, many") return "fail1: $result"
    if (log != "foo(1);foo(2);foo(3);three(3);foo(4);three(4);foo(5);three(5);foo(6);three(6);foo(7);three(7);") return "fail2: $log"

    return "OK"
}
