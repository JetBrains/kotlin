fun test(x: Any?, y: Double) =
    x is Int && x < y

fun box(): String =
    if (!test(0, -0.0))
        "OK"
    else
        "Failed"
