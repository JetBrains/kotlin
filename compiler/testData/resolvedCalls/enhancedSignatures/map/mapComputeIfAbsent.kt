fun valuesNotNull(map: MutableMap<Int, String>) {
    map.<caret>computeIfAbsent(1) { k -> "new value" }
}

fun valuesNullable(map: MutableMap<Int, String?>) {
    map.<caret>computeIfAbsent(1) { k -> null }
}

fun <T> valuesT(map: MutableMap<Int, T>, newValue: T) {
    map.<caret>computeIfAbsent(1) { k -> newValue }
}

fun <T : Any> valuesTNotNull(map: MutableMap<Int, T>, newValue: T) {
    map.<caret>computeIfAbsent(1) { k -> newValue }
}

fun <T : Any> valuesTNullable(map: MutableMap<Int, T?>, newValue: T?) {
    map.<caret>computeIfAbsent(1) { k -> newValue }
}