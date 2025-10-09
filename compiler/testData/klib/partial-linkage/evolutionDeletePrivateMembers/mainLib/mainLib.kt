class Y: X() 

fun lib(): String = when {
    X().bar() != "no private references after change" -> "fail 1"
    Y().bar() != "no private references after change" -> "fail 2"

    else -> "OK"
}

