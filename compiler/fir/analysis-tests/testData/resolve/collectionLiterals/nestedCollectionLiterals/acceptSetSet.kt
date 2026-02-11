// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80500
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB

fun <U> mutableFoo(x: MutableSet<MutableSet<U>>) { }

fun testMutableFoo() {
    <!CANNOT_INFER_PARAMETER_TYPE!>mutableFoo<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    mutableFoo<String>([])

    <!CANNOT_INFER_PARAMETER_TYPE!>mutableFoo<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>]<!>)
    mutableFoo<String>([[]])

//     type([["42"]]) <: MutableSet<MutableSet<Tv(U)>>
//      => outer CL expands to mutableSetOf<Tv(K)>(["42"])
//     MutableSet<Tv(K)> <: MutableSet<MutableSet<Tv(U)>>
//      => Tv(K) == MutableSet<Tv(U)>
//     type(["42"]) <: Tv(K)
//      => inner CL expands to mutableSetOf<Tv(L)>("42")
//     MutableSet<Tv(L)> <: Tv(K)
//      => Tv(L) == Tv(U)
//     String <: Tv(L)
//      => fix Tv(L) to String
    mutableFoo([["42"]])

    mutableFoo([["42"], [42]])
    mutableFoo([mutableSetOf("42"), [42]])
}

fun <T> foo(x: Set<Set<T>>) { }

fun testFoo() {
    <!CANNOT_INFER_PARAMETER_TYPE!>foo<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    foo<String>([])
    <!CANNOT_INFER_PARAMETER_TYPE!>foo<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>]<!>)
    foo<String>([[]])

    // type([["42"]]) <: Set<Set<Tv(U)>>
    //  => outer CL expands to setOf<Tv(K)>(["42"])
    // Set<Tv(K)> <: Set<Set<Tv(U)>>
    //  => Tv(K) <: Set<Tv(U)>
    // type(["42"]) <: Tv(K)
    //  => type(["42"]) <: Set<Tv(U)>
    //  => inner CL expands to setOf<Tv(L)>("42")
    // Set<Tv(L)> <: Tv(K)
    //  => Tv(L) <: Tv(U)
    // String <: Tv(L)
    //  => fix Tv(L) to String
    //  => fix Tv(U) to String
    //  => fix Tv(K) to Set<String>
    foo([["42"]])

    foo([["42"], [42]])
    foo([setOf("42")])
    foo<String>([setOf("42")])
    foo<String>([setOf()])
    foo([setOf("42"), [42]])

    // ambiguity because from expansion of outer CL LOWER(MutableSet), from foo UPPER(Set)
    foo([mutableSetOf("42"), <!AMBIGUOUS_COLLECTION_LITERAL!>[42]<!>])
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, integerLiteral, nullableType, stringLiteral,
typeParameter */
