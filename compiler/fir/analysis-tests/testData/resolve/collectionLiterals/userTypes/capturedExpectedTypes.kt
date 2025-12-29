// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals

class C {
    companion object {
        operator fun of(vararg xs: Int): C = C()
    }
}

interface Setter<U> {
    fun set(u: U)
}

fun test(
    d: Setter<in C>,
    e: Setter<out C>,
    f: Setter<*>,
) {
    d.set(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    e.set(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    f.set(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, collectionLiteral, companionObject, functionDeclaration,
inProjection, interfaceDeclaration, nullableType, objectDeclaration, operator, outProjection, starProjection,
typeParameter, vararg */
