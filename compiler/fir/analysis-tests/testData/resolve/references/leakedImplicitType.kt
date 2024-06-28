// ISSUE: KT-46072

// Case 1
class Foo {
    fun bar() {}
    fun f() = <!UNRESOLVED_REFERENCE!>Unresolved<!>()::bar // Type of Unresolved()::bar is implicit
}

// Case 2
interface IA
interface IB : IA

fun IA.extFun(x: IB) {}
fun IB.extFun(x: IA) {}

fun testWithExpectedType() {
    val extFun_AA_B: IA.(IA) -> Unit = IB::<!NONE_APPLICABLE!>extFun<!> // extFun is unresolved, type of IB::extFun is implicit
}
