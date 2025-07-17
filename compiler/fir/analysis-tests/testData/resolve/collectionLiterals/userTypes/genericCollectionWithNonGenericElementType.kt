// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList<T> {
    companion object {
        operator fun <T> of(vararg vals: String): MyList<T> = MyList<T>()
    }
}

fun <T> acceptList(l: MyList<T>) = Unit
fun acceptStringList(l: MyList<String>) = Unit

class A

fun test() {
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(MyList.<!CANNOT_INFER_PARAMETER_TYPE!>of<!>("1", "2", "3")) // should not pass
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(MyList.<!CANNOT_INFER_PARAMETER_TYPE!>of<!>("0", <!ARGUMENT_TYPE_MISMATCH!>A()<!>)) // should not pass
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>["1", "2", "3"]<!>) // should not pass
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>["0", A()]<!>) // should not pass
    acceptList(MyList.of<String>())
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>[]<!>) // should not pass

    acceptStringList(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>[]<!>)
    acceptStringList(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>["1", "2", "3"]<!>)
    acceptStringList(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>["0", null]<!>) // should not pass
    acceptStringList(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>["0", A()]<!>) // should not pass
    acceptStringList(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>["0"]<!>)

    acceptList<A>(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>[]<!>)
    acceptList<A>(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>["1", "2", "3"]<!>)
    acceptList<Nothing>(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>["1", "2", "3"]<!>)
    acceptList<Nothing?>(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>[]<!>)
    acceptList<Any?>(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>["0", A()]<!>) // should not pass
    acceptList<String?>(<!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>["0", null]<!>) // should not pass

}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, functionDeclaration, nullableType,
objectDeclaration, operator, stringLiteral, typeParameter, vararg */
