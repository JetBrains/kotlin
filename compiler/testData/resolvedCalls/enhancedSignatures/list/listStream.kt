fun <E : CharSequence> notNullValues(collection: List<E>) {
    collection.<caret>stream()
    collection.<caret>parallelStream()
}

fun <E : CharSequence> nullableValues(collection: List<E?>) {
    collection.<caret>stream()
    collection.<caret>parallelStream()
}

fun <E : CharSequence?> nullableValues2(collection: List<E>) {
    collection.<caret>stream()
    collection.<caret>parallelStream()
}