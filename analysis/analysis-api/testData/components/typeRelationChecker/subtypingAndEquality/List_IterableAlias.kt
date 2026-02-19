package test

typealias IterableAlias<T> = Iterable<T>

val l<caret_type1>ist: List<String> = emptyList()

val i<caret_type2>terable: IterableAlias<String> = emptyList()

// ARE_EQUAL: false
// ARE_EQUAL_LENIENT: false
// IS_SUBTYPE: true
// IS_SUBTYPE_LENIENT: true

// SUPERCLASS_ID: test/IterableAlias
// IS_CLASS_SUBTYPE: true
// IS_CLASS_SUBTYPE_LENIENT: true
