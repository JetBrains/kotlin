// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +CollectionLiterals

fun f1(x: Any) { }
fun f1(x: Sequence<Int>) { }

fun t1() {
    f1([])
    f1([1, 2, 3])
    f1(["!"])
    f1([42u])
    f1([42L])
}

fun f2(x: Any) { }
fun f2(x: Sequence<Int>?) { }

fun t2() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f2<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f2<!>([42])
    f2(["!"])
}

fun f3(x: Any) { }
fun <T> f3(x: Sequence<T>) { }

fun t3() {
    <!CANNOT_INFER_PARAMETER_TYPE!>f3<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    f3([42])
    f3([42, "!"])
}

fun f4(x: Any) { }
fun <T: CharSequence> f4(x: Sequence<T>) { }

fun t4() {
    f4([])
    f4(["!"])
    f4([1, 2, 3])
}

fun f5(x: Any) { }
fun <T> f5(x: Sequence<T>?) { }

fun t5() {
    f5(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    f5(["!"])
}

fun f6(x: Any?) { }
fun f6(x: Sequence<Int>?) { }

fun t6() {
    f6([])
    f6([1, 2, 3])
    f6(["!"])
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, intersectionType, nullableType, stringLiteral,
typeConstraint, typeParameter, unsignedLiteral */
