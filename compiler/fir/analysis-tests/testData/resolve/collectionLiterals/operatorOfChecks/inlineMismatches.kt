// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals

class MyList<T> {
    companion object {
        inline operator fun of(lam: () -> Int): MyList<Int> = MyList()
        inline operator fun of(noinline lam1: () -> Int, crossinline lam2: () -> Int): MyList<Int> = MyList()
        operator fun of(vararg lams: () -> Int): MyList<Int> = MyList()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, functionalType, inline, nullableType,
objectDeclaration, operator, typeParameter, vararg */
