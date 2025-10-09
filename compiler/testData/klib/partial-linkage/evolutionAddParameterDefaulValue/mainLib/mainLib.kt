fun lib(): String = when {
    foo("global") != "foo after change global" -> "fail 1"
    X("constructor").bar("member") != "bar after change member and constructor" -> "fail 2"
    else -> "OK"
}

