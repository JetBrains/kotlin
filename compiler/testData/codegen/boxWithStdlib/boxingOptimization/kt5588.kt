fun box() : String {
    val s = "notA"
    val id = when (s) {
        "a" -> 1
        else -> null
    }

    if (id == null) return "OK"
    return "fail"
}
