// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
error class E
error class E2
error class E3

class Box<out T>(t: T)

typealias TA2<T> = TA<TA<T>> | E3
typealias TA<T> = Box<T | E>

fun test1(): TA<Int> = TA(1)
fun test2(): TA<Int> = TA(E())
fun test3(): <!ILLEGAL_PROJECTION_IN_TYPEALIAS_EXPANSION!>TA<out Number><!> = TA(E())
fun test4(): <!ILLEGAL_PROJECTION_IN_TYPEALIAS_EXPANSION!>TA<in String><!> = TA(E())
fun test5(): <!ILLEGAL_PROJECTION_IN_TYPEALIAS_EXPANSION!>TA<*><!> = TA(E())
fun test6(): TA<E2> = TA(E())
fun test7(): TA<E2?> = TA(E())
fun test7_(): TA<E2?> = TA(null)
fun test8(): TA<String | E2> = TA(E())
fun test9(): TA<Nothing? | E2> = TA(E())
fun test10(): TA<E2 | E3> = TA(E())

fun callableReference() {
    val _ = TA<Int>::<!UNRESOLVED_REFERENCE!>size<!>
    val _ = <!ILLEGAL_PROJECTION_IN_TYPEALIAS_EXPANSION!>TA<out Number><!>::<!UNRESOLVED_REFERENCE!>size<!>
    val _ = <!ILLEGAL_PROJECTION_IN_TYPEALIAS_EXPANSION!>TA<in String><!>::<!UNRESOLVED_REFERENCE!>size<!>
    val _ = <!ILLEGAL_PROJECTION_IN_TYPEALIAS_EXPANSION!>TA<*><!>::<!UNRESOLVED_REFERENCE!>size<!>
}

typealias TAError<T> = Box<<!NON_ERROR_COMPONENT_WRONG_POSITION_IN_UNION_TYPE!>String | T<!>>
typealias TABoth<X, Y> = Box<<!NON_ERROR_COMPONENT_WRONG_POSITION_IN_UNION_TYPE!>X | Y<!>>

fun <T1 : TA<String>, T2 : TA<T2>> bounded(
    a: T1 | E2,
    b: T2 | E2,
    c: <!NON_ERROR_COMPONENT_WRONG_POSITION_IN_UNION_TYPE!>E2 | T2<!>,
) {}

fun <A, B : CharSequence?, C : RichError?> nullableBounds(
    a: TA<A>,
    b: TA<B>,
    c: TA<C>,
    c2: Box<C | E>
) {
    val x: Box<C | E> = Box(null)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, nullableType, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeParameter */
