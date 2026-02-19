fun lib(): String = when {
    X.W.qux != "this is in companion object" -> "fail 1"
    X.W.bar() != "something in N" -> "fail 2"
    X().foo() != "with companion" -> "fail 3"

    else -> "OK"
}

