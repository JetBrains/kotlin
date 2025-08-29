// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList {
    companion object {
        operator fun <T> of(vararg vals: T): MyList = MyList()
    }
}

class A

val globalLst: MyList = <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>

fun test() {
    val lst1: MyList = ["1", "2", "3"]
    val lst2: MyList = [null, "0"]
    val lst3: MyList = ["0", A()]
    val lst4: MyList = [A(), "0", null]
    val lst5: MyList = <!CANNOT_INFER_PARAMETER_TYPE!>[]<!> // should not pass

    var lst: MyList = [1, 2, 3]
    lst = <!CANNOT_INFER_PARAMETER_TYPE!>[]<!> // should not pass
    lst = [null]
    lst = [null, A()]
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, functionDeclaration, localProperty,
nullableType, objectDeclaration, operator, propertyDeclaration, stringLiteral, typeParameter, vararg */
