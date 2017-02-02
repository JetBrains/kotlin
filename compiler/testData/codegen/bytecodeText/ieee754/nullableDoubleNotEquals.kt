fun myNotEquals(a: Double?, b: Double?) = a != b

fun myNotEquals1(a: Double?, b: Double) = a != b

fun myNotEquals2(a: Double, b: Double?) = a != b

fun myNotEquals0(a: Double, b: Double) = a != b


fun box(): String {
    if (myNotEquals(null, null)) return "fail 1"
    if (!myNotEquals(null, 0.0)) return "fail 2"
    if (!myNotEquals(0.0, null)) return "fail 3"
    if (myNotEquals(0.0, 0.0)) return "fail 4"

    if (!myNotEquals1(null, 0.0)) return "fail 5"
    if (myNotEquals1(0.0, 0.0)) return "fail 6"

    if (!myNotEquals2(0.0, null)) return "fail 7"
    if (myNotEquals2(0.0, 0.0)) return "fail 8"

    if (myNotEquals0(0.0, 0.0)) return "fail 9"

    return "OK"
}

/*
 1 areEqual \(Ljava/lang/Double;Ljava/lang/Double;\)Z
 1 areEqual \(DLjava/lang/Double;\)Z
 1 areEqual \(Ljava/lang/Double;D\)Z
 3 areEqual*/
