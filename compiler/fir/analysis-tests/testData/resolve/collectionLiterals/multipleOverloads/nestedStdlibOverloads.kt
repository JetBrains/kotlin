// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +CollectionLiterals

fun f1(x: Set<Set<Int>>) { }
fun f1(x: Set<Set<String>>) { }

fun t1() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f1<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>]<!>) // ambiguity
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f1<!>([["!"]]) // ambiguity
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f1<!>([[1], [2], [3]]) // ambiguity
    f1([[1], setOf(2), [3]]) // int
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f1<!>([[1], setOf(), [3]]) // ambiguity
    f1([[<!ARGUMENT_TYPE_MISMATCH!>"1"<!>], setOf(2), [<!ARGUMENT_TYPE_MISMATCH!>"3"<!>]]) // int + type mismatch
    f1([["1"], setOf<String>(), ["3"]]) // string
}

fun f2(x: Set<Set<Int>>) { }
fun f2(x: Set<Set<Any>>) { }

fun t2() {
    f2([[]]) // int
    f2([[<!ARGUMENT_TYPE_MISMATCH!>"!"<!>]]) // int + type mismatch
    f2([[1], [2], [3]]) // int
    f2([[1], setOf(2), [3]]) // int
    f2([[1], setOf(), [3]]) // int
    f2([[1], setOf<String>(), [3]]) // any
    f2([[<!ARGUMENT_TYPE_MISMATCH!>"1"<!>], setOf(2), [<!ARGUMENT_TYPE_MISMATCH!>"3"<!>]]) // int + type mismatch
    f2([["1"], setOf("2"), ["3"]]) // any
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, intersectionType, stringLiteral */
