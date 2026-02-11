// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList {
    companion object {
        operator fun of(vararg vals: String): MyList = MyList()
    }
}

class A

val globalLst: MyList = []

fun test() {
    val lst1: MyList = []
    val lst2: MyList = ["1", "2", "3"]
    val lst3: MyList = [<!NULL_FOR_NONNULL_TYPE!>null<!>] // should not pass
    val lst4: MyList = ["0", <!ARGUMENT_TYPE_MISMATCH!>A()<!>] // should not pass

    var lst: MyList = []
    lst = ["1", "2", "3"]
    lst = [<!NULL_FOR_NONNULL_TYPE!>null<!>] // should not pass
    lst = ["0", <!ARGUMENT_TYPE_MISMATCH!>A()<!>] // should not pass

    val withoutSpecifiedType = <!UNRESOLVED_REFERENCE!>[]<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, functionDeclaration, localProperty,
objectDeclaration, operator, propertyDeclaration, stringLiteral, vararg */
