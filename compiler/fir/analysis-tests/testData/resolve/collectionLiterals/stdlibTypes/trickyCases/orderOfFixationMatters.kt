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
    myIn([], id([intArrayOf()]))
    myContains(id([intArrayOf()]), [])

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
    myInWithUB([], id([mutableSetOf<Int>()]))
    myContainsWithUB(id([mutableSetOf<Int>()]), <!AMBIGUOUS_COLLECTION_LITERAL!>[]<!>)
    myInWithUB(id([]), id([mutableSetOf<Int>()]))
    myContainsWithUB(id([mutableSetOf<Int>()]), id(<!AMBIGUOUS_COLLECTION_LITERAL!>[]<!>))
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, nullableType, starProjection, typeConstraint,
typeParameter */
