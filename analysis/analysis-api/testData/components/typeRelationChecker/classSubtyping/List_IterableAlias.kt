package test

typealias IterableAlias<T> = Iterable<T>

val l<caret>ist: List<String> = emptyList()

// CLASS_ID: test/IterableAlias
// IS_SUBTYPE: true
// IS_SUBTYPE_LENIENT: true
