// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals

class MyList<T> {
    companion object {
        suspend operator fun of(): MyList<Int> = MyList()
        operator fun of(vararg ints: Int): MyList<Int> = MyList()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, nullableType, objectDeclaration, operator,
suspend, typeParameter, vararg */
