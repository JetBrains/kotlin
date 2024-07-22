package test

typealias ListAlias<T> = List<T>

typealias NestedListAlias<T> = List<T>

val l<caret>ist: NestedListAlias<String> = emptyList()

// CLASS_ID: kotlin/collections/Iterable
// IS_SUBTYPE: true
// IS_SUBTYPE_LENIENT: true
