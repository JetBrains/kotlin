// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun String?.foo(): String {
    return this ?: ""
}

class MyClass {
    private var s: String? = null

    fun bar(): String {
        s = "42"
        return s.foo()
    }    
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, elvisExpression, funWithExtensionReceiver, functionDeclaration,
nullableType, propertyDeclaration, stringLiteral, thisExpression */
