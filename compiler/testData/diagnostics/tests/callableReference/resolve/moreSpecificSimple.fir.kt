// !CHECK_TYPE

interface IA
interface IB : IA

fun IA.extFun() {}
fun IB.extFun() {}

fun test() {
    val extFun = <!UNRESOLVED_REFERENCE!>IB::extFun<!>
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><IB.() -> Unit>(extFun)
}
