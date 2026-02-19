fun bool_yes(): Boolean = true

fun box(): String {
    if (!bool_yes()) return "FAIL !bool_yes()"

    return "OK"
}
