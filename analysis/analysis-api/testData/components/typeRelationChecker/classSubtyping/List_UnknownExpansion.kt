package test

typealias Alias = Unknown

val l<caret>ist: List<String> = emptyList()

// CLASS_ID: test/Alias
// IS_SUBTYPE: false
// IS_SUBTYPE_LENIENT: true
