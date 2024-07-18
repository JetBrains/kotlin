open class Animal

class Human : Animal()

val v<caret_type1>1: List<Human> = listOf()

val v<caret_type2>2: List<Humn> = listOf()

// ARE_EQUAL: false
// ARE_EQUAL_LENIENT: true
// IS_SUBTYPE: false
// IS_SUBTYPE_LENIENT: true
