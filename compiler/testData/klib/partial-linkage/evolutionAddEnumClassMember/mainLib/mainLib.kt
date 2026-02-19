fun lib(): String = when {
    X.values().map { it.name }.joinToString(", ") != "Y, Z, W" -> "fail 1"
    X.valueOf("W").name != "W" -> "fail 2"

    else -> "OK"
}

