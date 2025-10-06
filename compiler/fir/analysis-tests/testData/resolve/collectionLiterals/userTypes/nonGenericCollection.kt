// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList {
    companion object {
        operator fun of(vararg vals: String): MyList = MyList()
    }
}

fun acceptList(l: MyList) = Unit

class A

fun test() {
    acceptList(MyList.of("1", "2", "3"))
    acceptList(["1", "2", "3"])
    acceptList([])
    acceptList([<!NULL_FOR_NONNULL_TYPE!>null<!>]) // should not pass
    acceptList([<!ARGUMENT_TYPE_MISMATCH!>A()<!>]) // should not pass
    acceptList(["0", <!ARGUMENT_TYPE_MISMATCH!>A()<!>]) // should not pass
    acceptList(["0", <!NULL_FOR_NONNULL_TYPE!>null<!>]) // should not pass
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, functionDeclaration, localProperty,
nullableType, objectDeclaration, operator, propertyDeclaration, stringLiteral, vararg */
