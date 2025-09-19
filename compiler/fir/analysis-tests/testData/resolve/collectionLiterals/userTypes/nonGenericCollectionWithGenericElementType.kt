// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList {
    companion object {
        <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun <T> of(vararg vals: T): MyList = MyList()
    }
}

fun acceptList(l: MyList) = Unit

class A

fun test() {
    acceptList(MyList.of("1", "2", "3"))
    acceptList(MyList.of("0", A()))
    acceptList(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>["1", "2", "3"]<!>)
    acceptList(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>["0", A()]<!>)
    acceptList(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>["0", null]<!>)
    acceptList(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>["0", A(), null]<!>)
    acceptList(MyList.of<String>())
    acceptList(MyList.of<<!ILLEGAL_TYPE_ARGUMENT_FOR_VARARG_PARAMETER_WARNING!>Nothing<!>>())
    acceptList(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>[]<!>) // should not pass

    val lst1: MyList = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>["1", "2", "3"]<!>
    val lst2: MyList = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>[null, "0"]<!>
    val lst3: MyList = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>["0", A()]<!>
    val lst4: MyList = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>[A(), "0", null]<!>
    val lst5: MyList = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>[]<!> // should not pass
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, functionDeclaration, localProperty,
nullableType, objectDeclaration, operator, propertyDeclaration, stringLiteral, typeParameter, vararg */
