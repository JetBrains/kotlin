// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

class B {
    class Builder
}

typealias ApplyRestrictions = B.Builder.() -> B.Builder

fun applyRestrictions1(): ApplyRestrictions = TODO()
fun applyRestrictions2() = applyRestrictions1()
fun <K> applyRestrictions3(e: K) = applyRestrictions1()

fun buildB() {
    val a1 = applyRestrictions1()
    val a2 = applyRestrictions2()
    val a3 = applyRestrictions3("foo")

    B.Builder().<!UNRESOLVED_REFERENCE!>a1<!>()
    B.Builder().<!UNRESOLVED_REFERENCE!>a2<!>()
    B.Builder().<!UNRESOLVED_REFERENCE!>a3<!>()
}

// additional example from #KT-34820

class R
class P

typealias F = R.(P) -> Unit

fun guess(): F? = TODO()
fun consume(f: F) {}

fun problem() {
    val p = guess()
    <!INAPPLICABLE_CANDIDATE!>consume<!>(p ?: {})
}
