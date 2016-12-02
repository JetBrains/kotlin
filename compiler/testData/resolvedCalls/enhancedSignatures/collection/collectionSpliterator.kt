// TODO: Fix platform types
fun <E : CharSequence> notNullValues(collection: Collection<E>) {
    collection.<caret>spliterator()
}

fun <E : CharSequence> nullableValues(collection: Collection<E?>) {
    collection.<caret>spliterator()
}

fun <E : CharSequence?> nullableValues2(collection: Collection<E>) {
    collection.<caret>spliterator()
}