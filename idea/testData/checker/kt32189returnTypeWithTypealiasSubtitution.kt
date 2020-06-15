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
    val <warning descr="[UNUSED_VARIABLE] Variable 'a3' is never used">a3</warning> = applyRestrictions3("foo")

    B.Builder().a1()
    B.Builder().a2()
    B.Builder().<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: a3">a3</error>()
}
