fun box(): String {
    val array = intArray(11, 12, 13)
    val p = array get 0
    if (p != 11) return "fail 1: $p"

    val stringArray = array("OK", "FAIL")
    return stringArray get 0
}