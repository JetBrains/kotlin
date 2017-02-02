fun myEquals(a: Float?, b: Float?) = a == b

fun myEquals1(a: Float?, b: Float) = a == b

fun myEquals2(a: Float, b: Float?) = a == b

fun myEquals0(a: Float, b: Float) = a == b


fun box(): String {
    if (!myEquals(null, null)) return "fail 1"
    if (myEquals(null, 0.0F)) return "fail 2"
    if (myEquals(0.0F, null)) return "fail 3"
    if (!myEquals(0.0F, 0.0F)) return "fail 4"

    if (myEquals1(null, 0.0F)) return "fail 5"
    if (!myEquals1(0.0F, 0.0F)) return "fail 6"

    if (myEquals2(0.0F, null)) return "fail 7"
    if (!myEquals2(0.0F, 0.0F)) return "fail 8"

    if (!myEquals0(0.0F, 0.0F)) return "fail 9"

    return "OK"
}

// 1 areEqual \(Ljava/lang/Float;Ljava/lang/Float;\)Z
// 1 areEqual \(FLjava/lang/Float;\)Z
// 1 areEqual \(Ljava/lang/Float;F\)Z
// 3 areEqual