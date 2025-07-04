// RUN_PIPELINE_TILL: FRONTEND
interface IA
interface IB : IA

fun IA.extFun(x: IB) {}
fun IB.extFun(x: IA) {}

fun test() {
    val extFun1 = IA::extFun
    val extFun2 = IB::<!OVERLOAD_RESOLUTION_AMBIGUITY!>extFun<!>
}

fun testWithExpectedType() {
    // NB: should be resolved to kotlin/FunctionX, not kotlin/reflect/FunctionX
    val extFun_AB_A: IA.(IB) -> Unit = IA::extFun
    val extFun_AA_B: IA.(IA) -> Unit = IB::<!NONE_APPLICABLE!>extFun<!>
    val extFun_BB_A: IB.(IB) -> Unit = IA::extFun
    val extFun_BA_B: IB.(IA) -> Unit = IB::extFun
    val extFun_BB_B: IB.(IB) -> Unit = IB::<!OVERLOAD_RESOLUTION_AMBIGUITY!>extFun<!>
}

/* GENERATED_FIR_TAGS: callableReference, funWithExtensionReceiver, functionDeclaration, functionalType,
interfaceDeclaration, localProperty, propertyDeclaration, typeWithExtension */
