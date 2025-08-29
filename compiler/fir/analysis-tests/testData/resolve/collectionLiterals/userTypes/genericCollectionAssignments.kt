// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList<T> {
    companion object {
        operator fun <T> of(vararg vals: T): MyList<T> = MyList<T>()
    }
}

class A

val globalLst: MyList<Long> = [1, 2]

fun test() {
    val lst1: MyList<String> = []
    val lst2: MyList<String> = ["1", "2", "3"]
    val lst3: MyList<String?> = ["1", "2", "3"]
    val lst4: MyList<Any?> = ["1", "2", "3"]
    val lst5: MyList<Any?> = [null, A(), "0"]
    val lst6: MyList<String> = <!INITIALIZER_TYPE_MISMATCH!>[null, "0"]<!> // should not pass
    val lst7: MyList<A?> = <!INITIALIZER_TYPE_MISMATCH!>[null, "0", A()]<!> // should not pass
    val lst8: MyList<Nothing> = <!ILLEGAL_TYPE_ARGUMENT_FOR_VARARG_PARAMETER_WARNING!>[]<!>
    val lst9: MyList<Nothing> = <!INITIALIZER_TYPE_MISMATCH!>["1", "2", "3"]<!> // should not pass
    val lst10: MyList<Nothing?> = [null]

    var lst: MyList<String?> = []
    lst = ["1", "2", "3"]
    lst = [null, "0"]
    lst = [null]
    lst = <!ASSIGNMENT_TYPE_MISMATCH!>[A()]<!> // should not pass
    lst = <!ASSIGNMENT_TYPE_MISMATCH!>["0", null, A()]<!> // should not pass
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, localProperty, nullableType,
objectDeclaration, operator, propertyDeclaration, stringLiteral, typeParameter, vararg */
