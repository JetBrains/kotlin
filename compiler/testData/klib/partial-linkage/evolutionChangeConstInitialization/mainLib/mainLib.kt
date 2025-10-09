fun lib(): String = when {
    bar != 1 -> "fail 1"
    muc != "v1" -> "fail 2"
    X.tis != "v1" -> "fail 3"
    X.roo != 1 -> "fail 4"
    Y.zeb != 1 -> "fail 5"
    Y.loo != "v1" -> "fail 6"

    else -> "OK"
}

