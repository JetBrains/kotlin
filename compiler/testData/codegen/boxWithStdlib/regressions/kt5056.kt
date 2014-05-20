fun box(): String {
    val list = array("a", "c", "b").toSortedList()
    return if (list.toString() == "[a, b, c]") "OK" else "Fail: $list"
}
