fun valuesNotNull(map: MutableMap<Int, String>) {
    map.<caret>compute(1) { k, v -> null }
}

fun valuesNullable(map: MutableMap<Int, String?>) {
    map.<caret>compute(1) { k, v -> v?.let { it + k } }
}

fun <T> valuesT(map: MutableMap<Int, T>, newValue: T) {
    map.<caret>compute(1) { k, v -> null }
}

fun <T : Any> valuesTNotNull(map: MutableMap<Int, T>, newValue: T) {
    map.<caret>compute(1) { k, v -> null }
}

fun <T : Any> valuesTNullable(map: MutableMap<Int, T?>, newValue: T?) {
    map.<caret>compute(1) { k, v -> null }
}