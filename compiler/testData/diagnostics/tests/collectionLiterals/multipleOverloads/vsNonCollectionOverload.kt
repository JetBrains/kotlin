// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB

fun foo(x: String) { }
fun foo(x: List<Char>) { }

fun <T> bar(x: List<T>) { }
fun bar(x: String) { }

fun <T> baz(x: Set<T>) { }
fun baz(x: String) { }

fun test() {
    foo("abc")
    foo([])
    foo(['a', 'b', 'c'])

    bar("abc")
    <!CANNOT_INFER_PARAMETER_TYPE!>bar<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    bar(['a', 'b', 'c'])

    baz("abc")
    <!CANNOT_INFER_PARAMETER_TYPE!>baz<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    baz(['a', 'b', 'c'])
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, nullableType, stringLiteral, typeParameter */
