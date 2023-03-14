// WITH_STDLIB
// !LANGUAGE: -ForbidInferringPostponedTypeVariableIntoDeclaredUpperBound
// ISSUE: KT-50520

fun box(): String {
    buildList {
        val foo = { first() }
        add(0, foo)
    }
    return "OK"
}
