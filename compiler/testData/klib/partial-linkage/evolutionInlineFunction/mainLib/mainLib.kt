fun lib(): String = when {
    foo() != "inline global" -> "fail 1"
    X().foo() != "inline member" -> "fail 2"
    else -> "OK"
}

