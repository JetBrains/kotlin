// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals

class MyList<T> {
    companion object {
        infix operator fun of(int: Int): MyList<Int> = MyList()
        operator fun of(vararg ints: Int): MyList<Int> = MyList()
    }
}

fun test() {
    val infixUse = MyList of 42
    val operatorUse: MyList<Int> = [42]
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, infix, integerLiteral, localProperty,
nullableType, objectDeclaration, operator, propertyDeclaration, typeParameter, vararg */
