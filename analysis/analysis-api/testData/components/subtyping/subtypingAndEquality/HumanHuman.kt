open class Animal

class Human : Animal()

val v<caret_type1>1: Human = Human()

val v<caret_type2>2: Human = Human()

// ARE_EQUAL: true
// ARE_EQUAL_LENIENT: true
// IS_SUBTYPE: true
// IS_SUBTYPE_LENIENT: true
