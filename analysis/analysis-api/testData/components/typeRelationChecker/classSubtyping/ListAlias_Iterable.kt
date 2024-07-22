package test

typealias ListAlias<T> = List<T>

val l<caret>ist: ListAlias<String> = emptyList()

// CLASS_ID: kotlin/collections/Iterable
// IS_SUBTYPE: true
// IS_SUBTYPE_LENIENT: true
