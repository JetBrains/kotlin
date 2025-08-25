// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList {
    companion object {
        operator fun <T> of(vararg vals: T): MyList = MyList()
    }
}

class A

fun test() {
    val lst1: MyList = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>["1", "2", "3"]<!>
    val lst2: MyList = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>[null, "0"]<!>
    val lst3: MyList = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>["0", A()]<!>
    val lst4: MyList = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>[A(), "0", null]<!>
    val lst5: MyList = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>[]<!> // should not pass
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, functionDeclaration, localProperty,
nullableType, objectDeclaration, operator, propertyDeclaration, stringLiteral, typeParameter, vararg */
