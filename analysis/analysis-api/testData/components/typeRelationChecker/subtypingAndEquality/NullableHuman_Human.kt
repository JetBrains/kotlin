package test

open class Animal

class Human : Animal()

val v<caret_type1>1: Human? = Human()

val v<caret_type2>2: Human = Human()

// ARE_EQUAL: false
// ARE_EQUAL_LENIENT: false
// IS_SUBTYPE: false
// IS_SUBTYPE_LENIENT: false

// SUPERCLASS_ID: test/Human
// IS_CLASS_SUBTYPE: true
// IS_CLASS_SUBTYPE_LENIENT: true
