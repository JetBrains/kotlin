// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun String?.foo(): String {
    return this ?: ""
}

class MyClass {
    fun bar(): String {
        var s: String? = null
        if (4 < 2)
            s = "42"
        return s.foo()
    }    
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, comparisonExpression, elvisExpression, funWithExtensionReceiver,
functionDeclaration, ifExpression, integerLiteral, localProperty, nullableType, propertyDeclaration, stringLiteral,
thisExpression */
