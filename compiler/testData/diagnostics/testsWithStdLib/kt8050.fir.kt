// RUN_PIPELINE_TILL: BACKEND
private class X

private operator fun X?.plus(p: Int) = X()

class C {
    private val map = hashMapOf<String, X>()

    fun f() {
        map[""] += 1
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, funWithExtensionReceiver, functionDeclaration,
integerLiteral, localProperty, nullableType, operator, propertyDeclaration, stringLiteral */
