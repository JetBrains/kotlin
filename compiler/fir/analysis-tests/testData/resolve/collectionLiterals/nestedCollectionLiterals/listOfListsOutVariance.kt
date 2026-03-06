// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList<out T> {
    companion object {
        operator fun <K> of(vararg x: K): MyList<K> = MyList()
    }
}

fun <L> myListOf(vararg x: L): MyList<L> = []

fun <M> foo(lst: MyList<MyList<M>>) {
}

fun test() {
    foo<String>([[]])
    <!CANNOT_INFER_PARAMETER_TYPE!>foo<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>foo<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["42"]<!>]<!>)
    foo<String>([])
    <!CANNOT_INFER_PARAMETER_TYPE!>foo<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)

    foo([myListOf("42")])
    foo<String>([myListOf()])
    foo<String>([myListOf("42")])

    foo([myListOf("42"), []])
    <!CANNOT_INFER_PARAMETER_TYPE!>foo<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[<!CANNOT_INFER_PARAMETER_TYPE!>myListOf<!>(), <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["42"]<!>]<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, functionDeclaration, nullableType,
objectDeclaration, operator, out, stringLiteral, typeParameter, vararg */
