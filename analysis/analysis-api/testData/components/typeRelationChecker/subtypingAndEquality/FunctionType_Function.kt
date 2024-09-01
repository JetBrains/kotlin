package test

val f<caret_type1>unction1: (String) -> Int = { it.length }

val f<caret_type2>unction2: Function<Int> = function1

// ARE_EQUAL: false
// ARE_EQUAL_LENIENT: false
// IS_SUBTYPE: true
// IS_SUBTYPE_LENIENT: true

// SUPERCLASS_ID: kotlin/Function
// IS_CLASS_SUBTYPE: true
// IS_CLASS_SUBTYPE_LENIENT: true
