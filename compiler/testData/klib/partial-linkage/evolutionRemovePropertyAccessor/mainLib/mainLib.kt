fun lib(): String {
    val x = X()
    muc = "first"
    toc = "second"
    x.nis = "third"
    x.roo = "fourth"

    return when {
        bar != "changed global value" -> "fail 1"
        muc != "first" -> "fail 2"
        toc != "second" -> "fail 3"

        x.qux != "changed member value" -> "fail 4"
        x.nis != "third" -> "fail 5"
        x.roo != "fourth" -> "fail 6"

        else -> "OK"
    }
}

