// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun bar(s: Any): Int {
    return s.hashCode()
}

class MyClass(var p: Any) {
    fun foo(): Int {
        p = "xyz"
        if (p is String) {
            return bar(p)
        }
        return -1
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, ifExpression, integerLiteral, isExpression,
primaryConstructor, propertyDeclaration, smartcast, stringLiteral */
