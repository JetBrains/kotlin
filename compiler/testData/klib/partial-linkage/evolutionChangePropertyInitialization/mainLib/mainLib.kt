fun lib(): String {
    val x = X()

    return when {
        bar != 23 -> "fail 1"
        muc != "fifth" -> "fail 2"
        toc != "sixth" -> "fail 3"

        x.bar != "seventh" -> "fail 4"
        x.muc != 29 -> "fail 5"
        x.toc != "eighth" -> "fail 6"

        else -> "OK"
    }
}

