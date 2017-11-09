fun notNullValues(collection: MutableCollection<String>) {
    collection.<caret>removeIf { it.length > 5 }
}

fun <E : CharSequence> nullableValues(collection: MutableCollection<E?>) {
    collection.<caret>removeIf { it != null && it.length > 5 }
}

fun <E : CharSequence?> nullableValues2(collection: MutableCollection<E>) {
    collection.<caret>removeIf { it == null }
}