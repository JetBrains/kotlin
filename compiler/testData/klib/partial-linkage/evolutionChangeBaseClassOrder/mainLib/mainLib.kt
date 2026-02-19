fun lib(): String {
    val w = W()
    return when {
        w.bar != "from base class" -> "fail 1"
        w.zon != "base class" -> "fail 2"
        w.qux != "from interface Y" -> "fail 3"
        w.sep != "from interface Z" -> "fail 4"
        else -> "OK"
    }
}

