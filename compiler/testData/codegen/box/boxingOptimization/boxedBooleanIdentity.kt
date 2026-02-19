fun boxBoolean(b: Boolean): Any = b

fun box(): String {
    if (boxBoolean(true) !== boxBoolean(true)) return "FAIL1"
    if (boxBoolean(false) !== boxBoolean(false)) return "FAIL2"
    if (boxBoolean(true) === boxBoolean(false)) return "FAIL3"
    if (boxBoolean(false) === boxBoolean(true)) return "FAIL4"
    if (boxBoolean(true) != boxBoolean(true)) return "FAIL5"
    if (boxBoolean(false) != boxBoolean(false)) return "FAIL6"
    if (boxBoolean(true) == boxBoolean(false)) return "FAIL7"
    if (boxBoolean(false) == boxBoolean(true)) return "FAIL8"
    return "OK"
}