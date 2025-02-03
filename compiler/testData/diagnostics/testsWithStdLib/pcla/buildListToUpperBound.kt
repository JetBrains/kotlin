// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// WITH_STDLIB
// LANGUAGE: -ForbidInferringPostponedTypeVariableIntoDeclaredUpperBound
// ISSUE: KT-50520

fun box(): String {
    <!INFERRED_INTO_DECLARED_UPPER_BOUNDS!>buildList<!> {
        val foo = { first() }
        add(0, foo)
    }
    return "OK"
}
