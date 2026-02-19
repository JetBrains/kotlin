package test

val i<caret_type1>terable: Iterable<String> = emptyList()

val l<caret_type2>ist: List<String> = emptyList()

// ARE_EQUAL: false
// ARE_EQUAL_LENIENT: false
// IS_SUBTYPE: false
// IS_SUBTYPE_LENIENT: false

// SUPERCLASS_ID: kotlin/collections/List
// IS_CLASS_SUBTYPE: false
// IS_CLASS_SUBTYPE_LENIENT: false
