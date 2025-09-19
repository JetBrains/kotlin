// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList {
    companion object {
        operator fun of(): MyList = MyList()
        operator fun of(s: String): MyList = MyList()
        operator fun of(s: String, s2: String): MyList = MyList()
        operator fun of(s: String, s2: String, s3: String): MyList = MyList()
    }
}

class MyGenericList<T> {
    companion object {
        operator fun <T1> of(): MyGenericList<T1> = MyGenericList<T1>()
        operator fun <T1> of(t: T1): MyGenericList<T1> = MyGenericList<T1>()
        operator fun <T1> of(t: T1, t2: T1): MyGenericList<T1> = MyGenericList<T1>()
        operator fun <T1> of(t: T1, t2: T1, t3: T1): MyGenericList<T1> = MyGenericList<T1>()
    }
}

fun acceptList(l: MyList) { }
fun <T2> acceptGenericList(l: MyGenericList<T2>) { }

fun test() {
    acceptList(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["1", "2", "3"]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptGenericList<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["1", "2", "3"]<!>)
    acceptGenericList<String>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["1", "2", "3"]<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, nullableType, objectDeclaration, operator,
stringLiteral, typeParameter */
