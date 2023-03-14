// WITH_STDLIB
// !LANGUAGE: +ForbidInferringPostponedTypeVariableIntoDeclaredUpperBound
// ISSUE: KT-56169

fun box(): String {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>buildList<!> {
        val foo = { first() }
        add(0, foo)
    }
    return "OK"
}
