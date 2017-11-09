fun valuesNotNull(map: MutableMap<Int, String>) {
    map.<caret>putIfAbsent(1, "")
}

fun valuesNullable(map: MutableMap<Int, String?>) {
    map.<caret>putIfAbsent(1, null)
}
