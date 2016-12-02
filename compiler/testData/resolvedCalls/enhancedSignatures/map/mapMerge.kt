fun valuesNotNull(map: MutableMap<Int, String>) {
    map.<caret>merge(1, "x") { old, new -> old + new }
}

fun valuesNullable(map: MutableMap<Int, String?>) {
    map.<caret>merge(1, "x") { old, new -> old + new }
    map.<caret>merge(1, null) { old, new -> old + new }
}

fun <T> valuesT(map: MutableMap<Int, T>, newValue: T) {
    map.<caret>merge(1, newValue) { old, new -> null }
}

fun <T : Any> valuesTNotNull(map: MutableMap<Int, T>, newValue: T) {
    map.<caret>merge(1, newValue) { old, new -> null }
}

fun <T : Any> valuesTNullable(map: MutableMap<Int, T?>, newValue: T?) {
    map.<caret>merge(1, newValue) { old, new -> new }
    map.<caret>merge(1, newValue!!) { old, new -> new }
}