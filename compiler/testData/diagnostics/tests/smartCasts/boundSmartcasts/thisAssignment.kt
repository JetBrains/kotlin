// RUN_PIPELINE_TILL: BACKEND
interface A {
    fun foo()
}

fun Any.test() {
    if (this is A) {
        val a = this
        a.foo()
    }
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, ifExpression, interfaceDeclaration, isExpression,
localProperty, propertyDeclaration, smartcast, thisExpression */
