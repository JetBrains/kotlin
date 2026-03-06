// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals

class C<T> {
    companion object {
        operator fun <K> of(vararg ks: K): C<K> = C()

        fun <G> nullable(): C<G>? = C()
        fun <H> regular(): C<H> = C()
    }
}

fun <T> expectThroughTV(a: T, b: T & Any) {
}

fun test() {
    expectThroughTV(C.regular<Int>(), [])
    expectThroughTV(C.nullable<Int>(), [])
    <!CANNOT_INFER_PARAMETER_TYPE!>expectThroughTV<!>(C.<!CANNOT_INFER_PARAMETER_TYPE!>regular<!>(), <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>expectThroughTV<!>(C.<!CANNOT_INFER_PARAMETER_TYPE!>nullable<!>(), <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42]<!>)
    expectThroughTV(C.nullable<Int>(), <!ARGUMENT_TYPE_MISMATCH!>[42L]<!>)
    expectThroughTV(C.nullable<Long>(), [42])
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, dnnType, functionDeclaration,
integerLiteral, nullableType, objectDeclaration, operator, typeParameter, vararg */
