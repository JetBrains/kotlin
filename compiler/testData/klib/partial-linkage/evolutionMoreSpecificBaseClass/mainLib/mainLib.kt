fun lib(): String {
    return when {
        Z().bar != "child class" -> "fail 1"
        else -> "OK"
    }
}

