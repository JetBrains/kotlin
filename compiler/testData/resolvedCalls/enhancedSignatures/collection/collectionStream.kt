fun <E : CharSequence> notNullValues(collection: Collection<E>) {
    collection.<caret>stream()
    collection.<caret>parallelStream()
}

fun <E : CharSequence> nullableValues(collection: Collection<E?>) {
    collection.<caret>stream()
    collection.<caret>parallelStream()
}

fun <E : CharSequence?> nullableValues2(collection: Collection<E>) {
    collection.<caret>stream()
    collection.<caret>parallelStream()
}