fun lib(): String = when {
    qux() != "new member" -> "fail 1"

    else -> "OK"
}

