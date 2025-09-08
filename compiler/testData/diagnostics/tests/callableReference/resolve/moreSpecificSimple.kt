// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE

interface IA
interface IB : IA

fun IA.extFun() {}
fun IB.extFun() {}

fun test() {
    val extFun = IB::extFun
    checkSubtype<IB.() -> Unit>(extFun)
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, infix, interfaceDeclaration, localProperty, nullableType, propertyDeclaration, typeParameter,
typeWithExtension */
