// LANGUAGE_VERSION: 1.0

fun myEquals(a: Double?, b: Double?) = a == b

fun myEquals1(a: Double?, b: Double) = a == b

fun myEquals2(a: Double, b: Double?) = a == b

fun myEquals0(a: Double, b: Double) = a == b


fun box(): String {
    if (!myEquals(null, null)) return "fail 1"
    if (myEquals(null, 0.0)) return "fail 2"
    if (myEquals(0.0, null)) return "fail 3"
    if (!myEquals(0.0, 0.0)) return "fail 4"

    if (myEquals1(null, 0.0)) return "fail 5"
    if (!myEquals1(0.0, 0.0)) return "fail 6"

    if (myEquals2(0.0, null)) return "fail 7"
    if (!myEquals2(0.0, 0.0)) return "fail 8"

    if (!myEquals0(0.0, 0.0)) return "fail 9"

    return "OK"
}

// 0 areEquals