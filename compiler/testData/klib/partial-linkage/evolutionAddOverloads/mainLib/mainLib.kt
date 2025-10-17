val x = X()

fun lib(): String = when {
    x.foo("first") != "initial overload first" -> "fail 1"
    x.bar("second") != "initial overload second" -> "fail 2"
    x.qux("third") != "initial any overload third" -> "fail 3"

    foo("first") != "initial overload first" -> "fail 4"
    bar("second") != "initial overload second" -> "fail 5"
    qux("third") != "initial any overload third" -> "fail 6"

    else -> "OK"
}

