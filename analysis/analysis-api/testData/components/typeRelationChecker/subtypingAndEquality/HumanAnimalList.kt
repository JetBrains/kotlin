open class Animal

class Human : Animal()

val v<caret_type1>1: List<Human> = listOf()

val v<caret_type2>2: List<Animal> = listOf()

// ARE_EQUAL: false
// ARE_EQUAL_LENIENT: false
// IS_SUBTYPE: true
// IS_SUBTYPE_LENIENT: true
