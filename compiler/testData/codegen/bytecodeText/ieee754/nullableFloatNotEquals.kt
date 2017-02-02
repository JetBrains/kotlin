fun myNotEquals(a: Float?, b: Float?) = a != b

fun myNotEquals1(a: Float?, b: Float) = a != b

fun myNotEquals2(a: Float, b: Float?) = a != b

fun myNotEquals0(a: Float, b: Float) = a != b


fun box(): String {
    if (myNotEquals(null, null)) return "fail 1"
    if (!myNotEquals(null, 0.0F)) return "fail 2"
    if (!myNotEquals(0.0F, null)) return "fail 3"
    if (myNotEquals(0.0F, 0.0F)) return "fail 4"

    if (!myNotEquals1(null, 0.0F)) return "fail 5"
    if (myNotEquals1(0.0F, 0.0F)) return "fail 6"

    if (!myNotEquals2(0.0F, null)) return "fail 7"
    if (myNotEquals2(0.0F, 0.0F)) return "fail 8"

    if (myNotEquals0(0.0F, 0.0F)) return "fail 9"

    return "OK"
}

// 1 areEqual \(Ljava/lang/Float;Ljava/lang/Float;\)Z
// 1 areEqual \(FLjava/lang/Float;\)Z
// 1 areEqual \(Ljava/lang/Float;F\)Z
// 3 areEqual