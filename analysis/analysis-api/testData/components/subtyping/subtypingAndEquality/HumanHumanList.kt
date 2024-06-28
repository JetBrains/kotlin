open class Animal

class Human : Animal()

val v<caret_type1>1: List<Human> = listOf()

val v<caret_type2>2: List<Human> = listOf()

// ARE_EQUAL: true
// ARE_EQUAL_LENIENT: true
// IS_SUBTYPE: true
// IS_SUBTYPE_LENIENT: true
