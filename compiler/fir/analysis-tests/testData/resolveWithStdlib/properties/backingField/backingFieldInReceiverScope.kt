// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

class Scope {
    val a : List<String>
        field = mutableListOf("a", "b")

    private val b = mutableListOf("b", "c")

    fun Scope.usageInsideWithExtension() {
        a[0] = "a"
        this@Scope.a[0] = "a"
        this.a[0] = "a"
    }

    context(a: Scope)
    fun usageInsideWithContext() {
        a.a[0] = "a"
    }
}
fun Scope.usageOutsideWithExtension() {
    this.a<!NO_SET_METHOD!>[0]<!> = "a"
}

context(a: Scope)
fun usageOutsideWithContext() {
    a.a<!NO_SET_METHOD!>[0]<!> = "a"
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, explicitBackingField, funWithExtensionReceiver, functionDeclaration,
functionDeclarationWithContext, integerLiteral, propertyDeclaration, smartcast, stringLiteral, thisExpression */
