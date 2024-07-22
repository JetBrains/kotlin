package test

typealias IterableAlias<T> = Iterable<T>

typealias NestedIterableAlias<T> = IterableAlias<T>

val l<caret>ist: List<String> = emptyList()

// CLASS_ID: test/NestedIterableAlias
// IS_SUBTYPE: true
// IS_SUBTYPE_LENIENT: true
