fun lib(): String = when {
    X().foo("yes") != "foo after change to infix yes" -> "fail 1"
    bar("indeed") != "bar after change to tailrec indeed" -> "fail 2"
    X().qux("naturally") != "qux after change to infix naturally" -> "fail 3"
    X().muc("of course") != "muc after change to tailrec of course" -> "fail 4"
    else -> "OK"
}

