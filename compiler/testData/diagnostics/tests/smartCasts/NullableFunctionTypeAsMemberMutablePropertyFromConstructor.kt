// RUN_PIPELINE_TILL: FRONTEND
fun box(): String {
    Klass({})
    return "OK"
}

class Klass(var func: (() -> Unit)?) {
    init {
        if (func != null) {
            <!UNSAFE_IMPLICIT_INVOKE_CALL!>func<!>()
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, functionalType, ifExpression, init,
lambdaLiteral, nullableType, primaryConstructor, propertyDeclaration, smartcast, stringLiteral */
