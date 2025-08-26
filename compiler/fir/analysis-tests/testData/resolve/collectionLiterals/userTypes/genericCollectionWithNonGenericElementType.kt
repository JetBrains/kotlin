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
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(<!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>["1", "2", "3"]<!>) // should not pass
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(<!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>["0", A()]<!>) // should not pass
    acceptList(MyList.of<String>())
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(<!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>[]<!>) // should not pass

    acceptStringList([])
    acceptStringList(["1", "2", "3"])
    acceptStringList(["0", <!NULL_FOR_NONNULL_TYPE!>null<!>]) // should not pass
    acceptStringList(["0", <!ARGUMENT_TYPE_MISMATCH!>A()<!>]) // should not pass
    acceptStringList(["0"])

    acceptList<A>([])
    acceptList<A>(["1", "2", "3"])
    acceptList<Nothing>(["1", "2", "3"])
    acceptList<Nothing?>([])
    acceptList<Any?>(["0", <!ARGUMENT_TYPE_MISMATCH!>A()<!>]) // should not pass
    acceptList<String?>(["0", <!NULL_FOR_NONNULL_TYPE!>null<!>]) // should not pass
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, functionDeclaration, nullableType,
objectDeclaration, operator, stringLiteral, typeParameter, vararg */
