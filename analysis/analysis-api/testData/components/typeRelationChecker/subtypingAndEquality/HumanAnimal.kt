open class Animal

class Human : Animal()

val v<caret_type1>1: Human = Human()

val v<caret_type2>2: Animal = Animal()

// ARE_EQUAL: false
// ARE_EQUAL_LENIENT: false
// IS_SUBTYPE: true
// IS_SUBTYPE_LENIENT: true
