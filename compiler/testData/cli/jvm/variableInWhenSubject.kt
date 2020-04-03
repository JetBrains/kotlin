fun test(s: String?) =
    when (val v = s) {
        null -> ""
        else -> v
    }
