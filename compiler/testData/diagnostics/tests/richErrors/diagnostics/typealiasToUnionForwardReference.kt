// RUN_PIPELINE_TILL: FRONTEND
error class E1
error class E2

// `TA` is declared after `A`, so it has to be resolved on demand during the SUPER_TYPES phase
// to classify it as a rich error component.
typealias A = TA | E2
typealias TA = E1

fun a(): A = E1()
fun a2(): A = E2()
// Would be reported as NON_ERROR_COMPONENT_IN_NESTED_UNION_TYPE if `E1` were treated as the non-error component of `A`
fun nested(): Int | A = 1
fun nested2(): Int | A = E1()

fun test(a: A, n: Int | A) {
    val a1: E1 | E2 = a
    val n1: Int | E1 | E2 = n
}

// `TB` is declared after `B`, so it has to be resolved on demand during the SUPER_TYPES phase
// to detect the non-error component in the wrong position.
typealias B = <!NON_ERROR_COMPONENT_WRONG_POSITION_IN_UNION_TYPE!>E1 | TB<!>
typealias TB = String

typealias X1 = <!NON_ERROR_COMPONENT_IN_NESTED_UNION_TYPE!>String | X2<!>
typealias X3 = Int | E2
typealias X2 = X3 | E1

typealias Nested1 = List<<!NON_ERROR_COMPONENT_IN_NESTED_UNION_TYPE!>String | Nested2<!>>
typealias Nested2 = Int | E1

interface I<T>
interface J
class C : J, I<<!NON_ERROR_COMPONENT_IN_NESTED_UNION_TYPE!>E1 | ArgTa<!>>
typealias ArgTa = String | E2

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localProperty, propertyDeclaration,
typeAliasDeclaration */
