package test

open class Animal {
    private open class PrivateAnimal : Animal()

    private class Human : PrivateAnimal()

    val v<caret_type1>1: Human = Human()

    val v<caret_type2>2: PrivateAnimal = PrivateAnimal()
}

// ARE_EQUAL: false
// ARE_EQUAL_LENIENT: false
// IS_SUBTYPE: true
// IS_SUBTYPE_LENIENT: true

// SUPERCLASS_ID: test/Animal.PrivateAnimal
// IS_CLASS_SUBTYPE: true
// IS_CLASS_SUBTYPE_LENIENT: true
