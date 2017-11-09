fun <E : CharSequence> notNullValues(collection: Iterable<E>) {
    collection.<caret>spliterator()
}

fun <E : CharSequence> nullableValues(collection: Iterable<E?>) {
    collection.<caret>spliterator()
}

fun <E : CharSequence?> nullableValues2(collection: Iterable<E>) {
    collection.<caret>spliterator()
}