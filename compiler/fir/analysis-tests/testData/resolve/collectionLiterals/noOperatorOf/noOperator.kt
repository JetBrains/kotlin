// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList {
    companion object {
        fun of(vararg strs: String): MyList = MyList()
    }
}

class MyGenericList<T> {
    companion object {
        fun <T1> of(vararg t: T1): MyGenericList<T1> = MyGenericList<T1>()
    }
}

fun acceptList(l: MyList) { }
fun <T2> acceptGenericList(l: MyGenericList<T2>) { }

fun test() {
    acceptList(<!UNRESOLVED_REFERENCE!>["1", "2", "3"]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptGenericList<!>(<!UNRESOLVED_REFERENCE!>["1", "2", "3"]<!>)
    acceptGenericList<String>(<!UNRESOLVED_REFERENCE!>["1", "2", "3"]<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, nullableType, objectDeclaration,
stringLiteral, typeParameter, vararg */
