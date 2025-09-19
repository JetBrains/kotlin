// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList<T> {
    companion object {
        operator fun <T1> of(vararg vals: T1): MyList<T1> = MyList<T1>()
    }
}

fun <T2> acceptList(l: MyList<T2>) = Unit
fun acceptStringList(l: MyList<String>) = Unit

class A

fun test() {
    acceptList(MyList.of("1", "2", "3"))
    acceptList(MyList.of("0", A()))
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>["1", "2", "3"]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>["0", A()]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>["0", null]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>["0", A(), null]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>[]<!>) // should not pass
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>[null]<!>)

    acceptStringList(MyList.of("1", "2", "3"))
    acceptStringList(MyList.of())
    acceptStringList(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>["0", null]<!>) // should not pass
    acceptStringList(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>[A(), "0"]<!>) // should not pass
    acceptStringList(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>[]<!>)
    acceptStringList(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>["1", "2", "3"]<!>)

    acceptList<String>(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>[]<!>)
    acceptList<Any?>(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>["1", "2", "3"]<!>)
    acceptList<String>(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>["1", "2", "3"]<!>)
    acceptList<String?>(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>["1", "2", "3"]<!>)
    acceptList<Any?>(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>[null, A(), "0"]<!>)
    acceptList<String>(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>[null, "0"]<!>) // should not pass
    acceptList<String?>(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>[null, "0", A()]<!>) // should not pass
    acceptList<Nothing>(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>[]<!>)
    acceptList<Nothing?>(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>[null]<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, functionDeclaration, localProperty,
nullableType, objectDeclaration, operator, propertyDeclaration, stringLiteral, typeParameter, vararg */
