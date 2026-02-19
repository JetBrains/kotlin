// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

interface A {
    fun foo(): CharSequence?
}

interface B : A {
    override fun foo(): String
}

fun test(a: A) {
    if (a is B) {
        a.foo()
        a.foo().checkType { _<String>() }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, ifExpression,
infix, interfaceDeclaration, isExpression, lambdaLiteral, nullableType, override, smartcast, typeParameter,
typeWithExtension */
