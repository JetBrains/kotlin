package test

val v<caret_type1>1: Unknown = Unknown()

val v<caret_type2>2: List<String> = emptyList()

// ARE_EQUAL: false
// ARE_EQUAL_LENIENT: true
// IS_SUBTYPE: false
// IS_SUBTYPE_LENIENT: true

// SUPERCLASS_ID: kotlin/collections/List
// IS_CLASS_SUBTYPE: false
// IS_CLASS_SUBTYPE_LENIENT: true
