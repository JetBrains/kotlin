fun valuesNotNull(map: MutableMap<Int, String>) {
    map.<caret>computeIfPresent(1) { k, v -> v.length.toString() ?: null }
}

fun valuesNullable(map: MutableMap<Int, String?>) {
    map.<caret>computeIfPresent(1) { k, v -> v?.length?.toString() }
}

fun <T : String?> valuesT(map: MutableMap<Int, T>, newValue: T) {
    map.<caret>computeIfPresent(1) { k, v -> v?.length.toString() ?: null }
}

fun <T : Any> valuesTNotNull(map: MutableMap<Int, T>, newValue: T) {
    map.<caret>computeIfPresent(1) { k, v -> null }
}

fun <T : Any> valuesTNullable(map: MutableMap<Int, T?>, newValue: T?) {
    map.<caret>computeIfPresent(1) { k, v -> null }
}