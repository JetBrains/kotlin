fun valuesNotNull(map: MutableMap<Int, String>) {
    map.<caret>forEach { k, v -> }
}

fun <T> valuesT(map: MutableMap<Int, T>, newValue: T) {
    map.<caret>forEach { k, v -> }
}
