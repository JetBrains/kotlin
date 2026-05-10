// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +CollectionLiterals

fun fooInt(x: Sequence<Int>) { }
fun fooInt(x: Iterable<Int>) { }

fun testFooInt() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>fooInt<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>) // ambiguity
    <!NONE_APPLICABLE!>fooInt<!>(["!"]) // no applicable candidates
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>fooInt<!>([42]) // ambiguity
    <!NONE_APPLICABLE!>fooInt<!>([42, "!"]) // no applicable candidates
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>fooInt<!>([1, 2, 3]) // ambiguity
}

fun <T> fooOneGeneric(x: Sequence<T>) { }
fun fooOneGeneric(x: Iterable<String>) { }

fun testFooOneGeneric() {
    fooOneGeneric([]) // non-generic
    fooOneGeneric([42]) // generic
    fooOneGeneric(["!"]) // non-generic
    fooOneGeneric([42, "!"]) // generic
}

fun <T> fooBothGeneric(x: Sequence<T>) { }
fun <T> fooBothGeneric(x: Iterable<T>) { }

fun testFooBothGeneric() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>fooBothGeneric<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>) // ambiguity
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>fooBothGeneric<!>([null, "whatever"]) // ambiguity
}

fun <T> fooOtherGeneric(x: Iterable<T>) { }
fun fooOtherGeneric(x: Sequence<String>) { }

fun testFooOtherGeneric() {
    fooOtherGeneric([])
    fooOtherGeneric(["!"])
    fooOtherGeneric([42])
    fooOtherGeneric([42, "!"])
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, intersectionType, nullableType, stringLiteral, typeParameter */
