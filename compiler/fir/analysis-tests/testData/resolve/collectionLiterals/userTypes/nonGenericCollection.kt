// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList {
    companion object {
        <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun of(vararg vals: String): MyList = MyList()
    }
}

fun <T> acceptList(l: MyList<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><T><!>) = Unit

class A

fun test() {
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(MyList.of("1", "2", "3"))
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(<!UNSUPPORTED!>["1", "2", "3"]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(<!UNSUPPORTED!>[]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(<!UNSUPPORTED!>[null]<!>) // should not pass
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(<!UNSUPPORTED!>[A()]<!>) // should not pass
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(<!UNSUPPORTED!>["0", A()]<!>) // should not pass
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(<!UNSUPPORTED!>["0", null]<!>) // should not pass

    val lst1: MyList = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>[]<!>
    val lst2: MyList = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>["1", "2", "3"]<!>
    val lst3: MyList = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>[null]<!> // should not pass
    val lst4: MyList = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>["0", A()]<!> // should not pass
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, functionDeclaration, localProperty,
nullableType, objectDeclaration, operator, propertyDeclaration, stringLiteral, typeParameter, vararg */
