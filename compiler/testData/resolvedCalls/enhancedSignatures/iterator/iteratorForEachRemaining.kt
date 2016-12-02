fun notNullValues(it: Iterator<String>) {
    it.<caret>forEachRemaining { e -> }
}

fun mutableNullableValues(it: MutableIterator<String?>) {
    it.<caret>forEachRemaining { e -> }
}