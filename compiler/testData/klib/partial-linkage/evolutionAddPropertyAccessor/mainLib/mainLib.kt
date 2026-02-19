fun lib(): String {
    val x = X()
    muc = "first"
    toc = "second"
    x.nis = "third"
    x.roo = "fourth"

    return when {
        bar != "changed global value of val" -> "fail 1"
        muc != "changed global value of var with field" -> "fail 2"
        toc != "changed global value of var without field" -> "fail 3"

        x.qux != "changed member value of val" -> "fail 4"
        x.nis != "changed member value of var with field" -> "fail 5"
        x.roo != "changed member value of var without field" -> "fail 5"

        else -> "OK"
    }
}

