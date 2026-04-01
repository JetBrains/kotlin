// RUN_PIPELINE_TILL: BACKEND
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
    expectThroughTV(C.regular(), [42])
    expectThroughTV(C.nullable(), [42])
    expectThroughTV(C.nullable<Int>(), [42L])
    expectThroughTV(C.nullable<Long>(), [42])
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, dnnType, functionDeclaration,
integerLiteral, nullableType, objectDeclaration, operator, typeParameter, vararg */
