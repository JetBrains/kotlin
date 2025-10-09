fun lib(): String {
    val x = X()
    return when {
        x.x != 37 -> "fail 1"
        x.y != 41 -> "fail 2"
        else -> "OK"
    }
}

