// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
// CHECK_CASES_COUNT: function=sparse count=3
// CHECK_IF_COUNT: function=sparse count=0

fun sparse(x: Int): Int {
    return when ((x % 4) * 100) {
        100 -> 1
        200 -> 2
        300 -> 3
        else -> 4
    }
}

fun box(): String {
    var result = (0..3).map(::sparse).joinToString()

    if (result != "4, 1, 2, 3") return "sparse:" + result
    return "OK"
}
