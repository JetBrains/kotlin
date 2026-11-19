// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
error class E
error class E2
error class E3

class Box<out T>(t: T)

typealias TA2<T> = TA<TA<T>> | E3
typealias TA<T> = Box<T | E>

fun test1(): TA<Int> = Box(1)
fun test2(): TA<Int> = Box(E())
fun test3(): <!ILLEGAL_PROJECTION_IN_TYPEALIAS_EXPANSION!>TA<out Number><!> = Box(E())
fun test4(): <!ILLEGAL_PROJECTION_IN_TYPEALIAS_EXPANSION!>TA<in String><!> = Box(E())
fun test5(): <!ILLEGAL_PROJECTION_IN_TYPEALIAS_EXPANSION!>TA<*><!> = Box(E())
fun test6(): TA<E2> = Box(E())
fun test7(): TA<E2?> = Box(E())
fun test8(): TA<String | E2> = Box(E())
fun test9(): TA<Nothing? | E2> = Box(E())
fun test10(): TA<E2 | E3> = Box(E())

fun callableReference() {
    val _ = TA<Int>::<!UNRESOLVED_REFERENCE!>size<!>
    val _ = <!ILLEGAL_PROJECTION_IN_TYPEALIAS_EXPANSION!>TA<out Number><!>::<!UNRESOLVED_REFERENCE!>size<!>
    val _ = <!ILLEGAL_PROJECTION_IN_TYPEALIAS_EXPANSION!>TA<in String><!>::<!UNRESOLVED_REFERENCE!>size<!>
    val _ = <!ILLEGAL_PROJECTION_IN_TYPEALIAS_EXPANSION!>TA<*><!>::<!UNRESOLVED_REFERENCE!>size<!>
}

typealias TAError<T> = Box<<!NON_ERROR_COMPONENT_WRONG_POSITION_IN_UNION_TYPE!>String | T<!>>
typealias TABoth<X, Y> = Box<<!NON_ERROR_COMPONENT_WRONG_POSITION_IN_UNION_TYPE!>X | Y<!>>

fun <T1 : TA<String>> bounded(
    a: T1 | E2
) {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, nullableType, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeParameter */
