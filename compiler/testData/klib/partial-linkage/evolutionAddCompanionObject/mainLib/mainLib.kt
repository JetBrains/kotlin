fun lib(): String = when {
    X().foo() != "with companion" -> "fail 1"
    else -> "OK"
}

