fun lib(): String = when {
    X.qux != "this is in object" -> "fail 1"
    X.bar() != "something in N" -> "fail 2"
    X().foo() != "without companion" -> "fail 3"

    else -> "OK"
}

