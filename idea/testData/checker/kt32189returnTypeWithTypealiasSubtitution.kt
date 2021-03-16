// FIR_IDENTICAL
class B {
    class Builder
}

typealias ApplyRestrictions = B.Builder.() -> B.Builder

fun applyRestrictions1(): ApplyRestrictions = { this }
fun applyRestrictions2() = applyRestrictions1()
fun <K> applyRestrictions3(<warning descr="[UNUSED_PARAMETER] Parameter 'e' is never used">e</warning>: K) = applyRestrictions1()

fun buildB() {
    val a1 = applyRestrictions1()
    val a2 = applyRestrictions2()
    val a3 = applyRestrictions3("foo")

    B.Builder().a1()
    B.Builder().a2()
    B.Builder().a3()
}
