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
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(<!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>["1", "2", "3"]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(<!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>["0", A()]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(<!CANNOT_INFER_PARAMETER_TYPE!>["0", <!NULL_FOR_NONNULL_TYPE!>null<!>]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(<!CANNOT_INFER_PARAMETER_TYPE!>["0", A(), <!NULL_FOR_NONNULL_TYPE!>null<!>]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(<!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>[]<!>) // should not pass
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptList<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[<!NULL_FOR_NONNULL_TYPE!>null<!>]<!>)

    acceptStringList(MyList.of("1", "2", "3"))
    acceptStringList(MyList.of())
    acceptStringList(<!ARGUMENT_TYPE_MISMATCH!>["0", null]<!>) // should not pass
    acceptStringList(<!ARGUMENT_TYPE_MISMATCH!>[A(), "0"]<!>) // should not pass
    acceptStringList([])
    acceptStringList(["1", "2", "3"])

    acceptList<String>([])
    acceptList<Any?>(["1", "2", "3"])
    acceptList<String>(["1", "2", "3"])
    acceptList<String?>(["1", "2", "3"])
    acceptList<Any?>([null, A(), "0"])
    acceptList<String>(<!ARGUMENT_TYPE_MISMATCH!>[null, "0"]<!>) // should not pass
    acceptList<String?>(<!ARGUMENT_TYPE_MISMATCH!>[null, "0", A()]<!>) // should not pass
    acceptList<Nothing>(<!ILLEGAL_TYPE_ARGUMENT_FOR_VARARG_PARAMETER_WARNING!>[]<!>)
    acceptList<Nothing?>([null])
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, functionDeclaration, localProperty,
nullableType, objectDeclaration, operator, propertyDeclaration, stringLiteral, typeParameter, vararg */
