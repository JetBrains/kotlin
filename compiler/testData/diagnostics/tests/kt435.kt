// RUN_PIPELINE_TILL: FRONTEND
fun Any.foo1() : (i : Int) -> Unit {
    return {}
}

fun test(a : Any) {
    <!NO_VALUE_FOR_PARAMETER!>a.foo1()<!>()
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, lambdaLiteral */
