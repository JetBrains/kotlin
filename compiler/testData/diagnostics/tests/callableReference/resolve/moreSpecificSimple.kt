// !CHECK_TYPE

interface IA
interface IB : IA

fun IA.extFun() {}
fun IB.extFun() {}

fun test() {
    val extFun = IB::extFun
    checkSubtype<IB.() -> Unit>(extFun)
}
