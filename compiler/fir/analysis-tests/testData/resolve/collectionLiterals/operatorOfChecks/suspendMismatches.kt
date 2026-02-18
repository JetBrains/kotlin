// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals
// RENDER_DIAGNOSTICS_FULL_TEXT

class MyList<T> {
    companion object {
        <!INCONSISTENT_SUSPEND_IN_OF_OVERLOADS!>suspend<!> operator fun of(): MyList<Int> = MyList()
        operator fun of(vararg ints: Int): MyList<Int> = MyList()
    }
}

class AnotherList<T> {
    companion object {
        operator fun <!INCONSISTENT_SUSPEND_IN_OF_OVERLOADS!>of<!>(): AnotherList<Int> = AnotherList()
        suspend operator fun of(vararg ints: Int): AnotherList<Int> = AnotherList()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, nullableType, objectDeclaration, operator,
suspend, typeParameter, vararg */
