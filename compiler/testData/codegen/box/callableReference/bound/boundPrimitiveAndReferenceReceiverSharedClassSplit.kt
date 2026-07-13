fun Int.append(s: String): String = toString() + s

fun String.append(s: String): String = this + s

fun box(): String {
    val intReceiver: (String) -> String = 1::append
    val stringReceiver: (String) -> String = "O"::append

    val first = intReceiver("K")
    if (first != "1K") return "FAIL int receiver: $first"

    val second = stringReceiver("K")
    if (second != "OK") return "FAIL string receiver: $second"

    return "OK"
}
