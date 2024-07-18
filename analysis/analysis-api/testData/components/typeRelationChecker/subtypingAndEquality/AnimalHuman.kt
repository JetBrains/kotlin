open class Animal

class Human : Animal()

val v<caret_type1>1: Animal = Animal()

val v<caret_type2>2: Human = Human()

// ARE_EQUAL: false
// ARE_EQUAL_LENIENT: false
// IS_SUBTYPE: false
// IS_SUBTYPE_LENIENT: false
