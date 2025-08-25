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
    acceptList(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>["1", "2", "3"]<!>)
    acceptList(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>[]<!>)
    acceptList(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>[null]<!>) // should not pass
    acceptList(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>[A()]<!>) // should not pass
    acceptList(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>["0", A()]<!>) // should not pass
    acceptList(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>["0", null]<!>) // should not pass
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, functionDeclaration, localProperty,
nullableType, objectDeclaration, operator, propertyDeclaration, stringLiteral, vararg */
