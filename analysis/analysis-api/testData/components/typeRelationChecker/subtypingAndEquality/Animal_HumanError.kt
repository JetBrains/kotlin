package test

open class Animal

class Human : Animal()

val v<caret_type1>1: Animal = Animal()

val v<caret_type2>2: Humn = Human()

// ARE_EQUAL: false
// ARE_EQUAL_LENIENT: true
// IS_SUBTYPE: false
// IS_SUBTYPE_LENIENT: true

// SUPERCLASS_ID: test/Humn
// IS_CLASS_SUBTYPE: false
// IS_CLASS_SUBTYPE_LENIENT: true
