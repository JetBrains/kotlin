// RUN_PIPELINE_TILL: BACKEND
// DUMP_CFG
interface I

class C : I {
    fun I.foo() = "ret"
}

fun I.bar() {
    (this as C).foo()
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration,
interfaceDeclaration, smartcast, stringLiteral, thisExpression */
