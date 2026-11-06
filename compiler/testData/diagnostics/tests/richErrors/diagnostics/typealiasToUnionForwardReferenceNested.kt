// RUN_PIPELINE_TILL: FRONTEND
error class E1
error class E2

// Nested type aliases declared later have to be resolved on demand during the SUPER_TYPES phase,
// which requires the supertypes of their containing classes.
typealias A = Outer.TA | E2
typealias B = <!NON_ERROR_COMPONENT_WRONG_POSITION_IN_UNION_TYPE!>E1 | Outer.TB<!>

class Outer {
    typealias TA = E1
    typealias TB = String

    typealias C = <!NON_ERROR_COMPONENT_WRONG_POSITION_IN_UNION_TYPE!>E1 | TB<!>
    typealias D = Later.TD | E2
}

class Later {
    typealias TD = E1
}

fun a(): A = E1()
fun a2(): A = E2()
fun d(): Outer.D = E1()
fun nested(): Int | A = 1

fun test(a: A, d: Outer.D) {
    val a1: E1 | E2 = a
    val d1: E1 | E2 = d
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localProperty, propertyDeclaration,
typeAliasDeclaration */
