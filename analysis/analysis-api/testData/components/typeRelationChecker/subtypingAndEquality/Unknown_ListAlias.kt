package test

typealias ListAlias<T> = List<T>

val v<caret_type1>1: Unknown = Unknown()

val v<caret_type2>2: ListAlias<String> = emptyList()

// ARE_EQUAL: false
// ARE_EQUAL_LENIENT: true
// IS_SUBTYPE: false
// IS_SUBTYPE_LENIENT: true

// SUPERCLASS_ID: test/ListAlias
// IS_CLASS_SUBTYPE: false
// IS_CLASS_SUBTYPE_LENIENT: true
