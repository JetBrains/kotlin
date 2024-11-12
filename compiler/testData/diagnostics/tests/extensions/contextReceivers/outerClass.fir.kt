// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers
// DIAGNOSTICS: -UNUSED_PARAMETER

class Outer {
    val x: Int = 1
}

context(Outer)
class Inner(arg: Any) {
    fun bar() = x
}

fun f(outer: Outer) {
    <!NO_CONTEXT_ARGUMENT!>Inner<!>(1)
    with(outer) {
        Inner(3)
    }
}
