// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList<T> {
    companion object {
        operator fun <T> of(vararg vals: String): MyList<T> = MyList<T>()
    }
}

class A

val globalLst: MyList<*> = <!CANNOT_INFER_PARAMETER_TYPE!>["1", "2", "3"]<!>

fun test() {
    val lst1: MyList<String> = ["1", "2", "3"]
    val lst2: MyList<Any> = ["1", "2", "3"]
    val lst3: MyList<A> = ["1", "2", "3"]
    val lst4: MyList<String> = []
    val lst5: MyList<A> = [<!ARGUMENT_TYPE_MISMATCH!>A()<!>] // should not pass
    val lst6: MyList<Any> = [<!ARGUMENT_TYPE_MISMATCH!>A()<!>, "0"] // should not pass
    val lst7: MyList<Any?> = [<!NULL_FOR_NONNULL_TYPE!>null<!>, "0", <!ARGUMENT_TYPE_MISMATCH!>A()<!>] // should not pass

    var lst: MyList<Int> = ["1", "2", "3"]
    lst = [<!ARGUMENT_TYPE_MISMATCH!>A()<!>] // should not pass
    lst = []
    lst = [<!NULL_FOR_NONNULL_TYPE!>null<!>] // should not pass
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, functionDeclaration, localProperty,
nullableType, objectDeclaration, operator, propertyDeclaration, stringLiteral, typeParameter, vararg */
