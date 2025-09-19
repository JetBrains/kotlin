// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList {
    companion object {
        operator fun of(vararg vals: String): MyList = MyList()
    }
}

class A

fun test() {
    val lst1: MyList = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>[]<!>
    val lst2: MyList = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>["1", "2", "3"]<!>
    val lst3: MyList = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>[null]<!> // should not pass
    val lst4: MyList = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>["0", A()]<!> // should not pass
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, functionDeclaration, localProperty,
objectDeclaration, operator, propertyDeclaration, stringLiteral, vararg */
