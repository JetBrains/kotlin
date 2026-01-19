// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals

fun <K> id(k: K) = k

fun <T> myIn(b: T, a: Set<T>) { }
fun <T> myContains(a: Set<T>, b: T) { }

fun <T: Set<*>> myInWithUB(b: T, a: Set<T>) { }
fun <T: Set<*>> myContainsWithUB(a: Set<T>, b: T) { }

fun test() {
    // K <: Set<T>
    // typeOf([intArrayOf()]) <: K
    // typeOf([]) <: T

    // no nice constraints on empty literal => fixing non-empty first
    <!CANNOT_INFER_PARAMETER_TYPE!>myIn<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>, <!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[intArrayOf()]<!>))
    <!CANNOT_INFER_PARAMETER_TYPE!>myContains<!>(<!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[intArrayOf()]<!>), <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)

    /* ===== */

    // K <: Set<T>
    // typeOf([mutableSetOf<Int>()]) <: K
    // typeOf([]) <: T
    // T <: Set<*>
    //
    // Option 1.
    //   [] expands to setOf<L>()
    //   Set<L> <: T
    //   [...] expands to setOf(mutableSetOf<Int>())
    //   Set<MutableSet<Int>> <: K
    //   MutableSet<Int> <: T
    //   T := Set<Int>
    //
    //   Result: OK.
    // Option 2.
    //   [...] expands to setOf(mutableSetOf<Int>())
    //   Set<MutableSet<Int>> <: K
    //   MutableSet<Int> <: T
    //
    //   Result: Ambiguity for [].
    <!CANNOT_INFER_PARAMETER_TYPE!>myInWithUB<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>, <!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[mutableSetOf<Int>()]<!>))
    <!CANNOT_INFER_PARAMETER_TYPE!>myContainsWithUB<!>(<!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[mutableSetOf<Int>()]<!>), <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    myInWithUB(id(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>), id([mutableSetOf<Int>()]))
    myContainsWithUB(id([mutableSetOf<Int>()]), id(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>))
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, nullableType, starProjection, typeConstraint,
typeParameter */
