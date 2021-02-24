// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

interface IA
interface IB : IA

fun IA.extFun(x: IB) {}
fun IB.extFun(x: IA) {}

fun test() {
    val extFun1 = IA::extFun
    val extFun2 = <!UNRESOLVED_REFERENCE!>IB::extFun<!>
}

fun testWithExpectedType() {
    val extFun_AB_A: IA.(IB) -> Unit = IA::extFun
    val extFun_AA_B: IA.(IA) -> Unit = <!UNRESOLVED_REFERENCE!>IB::extFun<!>
    val extFun_BB_A: IB.(IB) -> Unit = IA::extFun
    val extFun_BA_B: IB.(IA) -> Unit = IB::extFun
    val extFun_BB_B: IB.(IB) -> Unit = <!UNRESOLVED_REFERENCE!>IB::extFun<!>
}