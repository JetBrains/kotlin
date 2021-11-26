// WITH_STDLIB
// CHECK_CASES_COUNT: function=foo count=9
// CHECK_IF_COUNT: function=foo count=0

fun foo(x: Int): Int {
    return when (x) {
        1, 2, 3 -> 1
        4, 5, 6 -> 2
        7, 8, 9 -> 3
        else -> 4
    }
}

fun box(): String {
    var result = (0..10).map(::foo).joinToString()

    if (result != "4, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4") return result
    return "OK"
}
