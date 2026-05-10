// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals

class MyList<T> {
    companion object {
        operator fun of(): MyList<Int> = MyList()
        tailrec operator fun of(vararg lams: Int): MyList<Int> {
            if (lams.size == 1) return []
            val x: MyList<Int> = <!NON_TAIL_RECURSIVE_CALL!>[1, 2, 3]<!>
            takeLst(<!NON_TAIL_RECURSIVE_CALL!>[1, 2, 3]<!>)
            return [lams[0]]
        }
    }
}

fun takeLst(lst: MyList<Int>) { }

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, nullableType, objectDeclaration, operator,
suspend, typeParameter, vararg */
