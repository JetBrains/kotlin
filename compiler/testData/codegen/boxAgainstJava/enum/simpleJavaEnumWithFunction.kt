import test.simpleJavaEnumWithFunction.*

fun box() =
    if (A.repr() == "A" && B.repr() == "olololB") "OK"
    else "fail"
