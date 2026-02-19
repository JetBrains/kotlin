// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList {
    companion object {
        <!NO_VARARG_OVERLOAD_OF_OPERATOR_OF!>operator fun of(): MyList<!> = MyList()
        <!NO_VARARG_OVERLOAD_OF_OPERATOR_OF!>operator fun of(s: String): MyList<!> = MyList()
        <!NO_VARARG_OVERLOAD_OF_OPERATOR_OF!>operator fun of(s: String, s2: String): MyList<!> = MyList()
        <!NO_VARARG_OVERLOAD_OF_OPERATOR_OF!>operator fun of(s: String, s2: String, s3: String): MyList<!> = MyList()
    }
}

class MyGenericList<T> {
    companion object {
        <!NO_VARARG_OVERLOAD_OF_OPERATOR_OF!>operator fun <T1> of(): MyGenericList<T1><!> = MyGenericList<T1>()
        <!NO_VARARG_OVERLOAD_OF_OPERATOR_OF!>operator fun <T1> of(t: T1): MyGenericList<T1><!> = MyGenericList<T1>()
        <!NO_VARARG_OVERLOAD_OF_OPERATOR_OF!>operator fun <T1> of(t: T1, t2: T1): MyGenericList<T1><!> = MyGenericList<T1>()
        <!NO_VARARG_OVERLOAD_OF_OPERATOR_OF!>operator fun <T1> of(t: T1, t2: T1, t3: T1): MyGenericList<T1><!> = MyGenericList<T1>()
    }
}

fun acceptList(l: MyList) { }
fun <T2> acceptGenericList(l: MyGenericList<T2>) { }

fun test() {
    acceptList(<!UNRESOLVED_REFERENCE!>["1", "2", "3"]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptGenericList<!>(<!UNRESOLVED_REFERENCE!>["1", "2", "3"]<!>)
    acceptGenericList<String>(<!UNRESOLVED_REFERENCE!>["1", "2", "3"]<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, nullableType, objectDeclaration, operator,
stringLiteral, typeParameter */
