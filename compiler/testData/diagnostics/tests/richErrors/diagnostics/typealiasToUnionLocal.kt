// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +LocalTypeAliases
error class E1
error class E2

fun test() {
    // The non-local type aliases are already resolved when the local type aliases are resolved,
    // even though they are declared later.
    typealias Loc = TA | E2
    typealias Loc2 = <!NON_ERROR_COMPONENT_WRONG_POSITION_IN_UNION_TYPE!>E1 | TS<!>

    val a: Loc = E1()
    val a1: E1 | E2 = a
}

typealias TA = E1
typealias TS = String

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, propertyDeclaration, typeAliasDeclaration */
