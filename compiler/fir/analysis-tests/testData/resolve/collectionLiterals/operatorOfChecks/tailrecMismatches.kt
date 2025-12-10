// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals

class MyList<T> {
    companion object {
        operator fun of(): MyList<Int> = MyList()
        // there must not be a warning: KT-83040
        <!NO_TAIL_CALLS_FOUND!>tailrec<!> operator fun of(vararg lams: Int): MyList<Int> {
            if (lams.size == 1) return []
            return [lams[0]]
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, nullableType, objectDeclaration, operator,
suspend, typeParameter, vararg */
