package test

typealias ListAlias<T> = List<T>

val v<caret>alue: Unknown = Unknown()

// CLASS_ID: test/ListAlias
// IS_SUBTYPE: false
// IS_SUBTYPE_LENIENT: true
