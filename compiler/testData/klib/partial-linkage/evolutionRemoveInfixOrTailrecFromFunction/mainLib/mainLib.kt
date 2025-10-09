fun lib(): String = when {
    X() foo "yes" != "foo after removal of infix yes" -> "fail 1"
    bar("indeed") != "bar after removal of tailrec indeed" -> "fail 2"
    X() qux "naturally" != "qux after removal of infix naturally" -> "fail 3"
    X().muc("of course") != "muc after removal of tailrec of course" -> "fail 4"
    else -> "OK"
}

