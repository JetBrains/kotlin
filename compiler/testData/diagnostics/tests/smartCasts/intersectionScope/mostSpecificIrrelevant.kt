// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

interface A {
    fun foo(): CharSequence?
}

interface B {
    fun foo(): String
}

fun test(c: Any) {
    if (c is B && c is A) {
        <!DEBUG_INFO_SMARTCAST!>c<!>.foo().checkType { _<String>() }
    }
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
ifExpression, infix, interfaceDeclaration, intersectionType, isExpression, lambdaLiteral, nullableType, smartcast,
typeParameter, typeWithExtension */
