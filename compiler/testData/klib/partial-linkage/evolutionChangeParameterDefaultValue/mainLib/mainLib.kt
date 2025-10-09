fun lib(): String = when {
    foo("in global") != "foo after change in global" -> "fail 1"
    X("in constructor").bar("in member") != "bar after change in member and in constructor" -> "fail 2"
    foo() != "foo after change in file" -> "fail 3"
    X().bar() != "bar after change in class and in constructor" -> "fail 4"
    else -> "OK"
}

