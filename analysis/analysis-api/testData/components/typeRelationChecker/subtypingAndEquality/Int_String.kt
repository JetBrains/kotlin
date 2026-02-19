package test

val v<caret_type1>1: Int = 1

val v<caret_type2>2: String = ""

// ARE_EQUAL: false
// ARE_EQUAL_LENIENT: false
// IS_SUBTYPE: false
// IS_SUBTYPE_LENIENT: false

// SUPERCLASS_ID: kotlin/String
// IS_CLASS_SUBTYPE: false
// IS_CLASS_SUBTYPE_LENIENT: false
