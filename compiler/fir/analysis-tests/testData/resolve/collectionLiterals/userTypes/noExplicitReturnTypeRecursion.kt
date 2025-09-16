// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList {
    companion object {
        operator fun of(vararg lst: MyList) = id(<!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>[MyList()]<!>)
    }
}

fun id(x: MyList): MyList = x

fun test() {
    val a: MyList = [[[[MyList()]]]]
    val b: MyList = MyList.of(MyList())
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, objectDeclaration, operator, vararg */
