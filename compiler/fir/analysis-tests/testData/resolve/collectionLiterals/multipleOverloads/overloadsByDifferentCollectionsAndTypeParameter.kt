// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList<T> {
    companion object {
        operator fun <T1> of(vararg vals: T1): MyList<T1> = MyList<T1>()
    }
}

class OtherList<K> {
    companion object {
        operator fun <K1> of(vararg vals: K1): OtherList<K1> = OtherList<K1>()
    }
}

fun acceptList(l: MyList<String>) = Unit
fun acceptList(l: OtherList<Int>) = Unit

fun test() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>acceptList<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>)
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>acceptList<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["1", "2", "3"]<!>)
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>acceptList<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, integerLiteral, nullableType,
objectDeclaration, operator, stringLiteral, typeParameter, vararg */
