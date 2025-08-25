// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList<T> {
    companion object {
        operator fun <T> of(vararg vals: String): MyList<T> = MyList<T>()
    }
}

class A

fun test() {
    val lst1: MyList<String> = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>["1", "2", "3"]<!>
    val lst2: MyList<Any> = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>["1", "2", "3"]<!>
    val lst3: MyList<A> = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>["1", "2", "3"]<!>
    val lst4: MyList<String> = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>[]<!>
    val lst5: MyList<A> = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>[A()]<!> // should not pass
    val lst6: MyList<Any> = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>[A(), "0"]<!> // should not pass
    val lst7: MyList<Any?> = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>[null, "0", A()]<!> // should not pass
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, functionDeclaration, localProperty,
nullableType, objectDeclaration, operator, propertyDeclaration, stringLiteral, typeParameter, vararg */
