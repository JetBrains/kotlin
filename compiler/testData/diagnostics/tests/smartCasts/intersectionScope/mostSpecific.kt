// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

interface Common {
    fun foo(): CharSequence?
}

interface A : Common {
    override fun foo(): CharSequence
}

interface B : Common {
    override fun foo(): String
}

fun test(c: Common) {
    if (c is B && c is A) {
        <!DEBUG_INFO_SMARTCAST!>c<!>.foo().checkType { _<String>() }
    }
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
ifExpression, infix, interfaceDeclaration, intersectionType, isExpression, lambdaLiteral, nullableType, override,
smartcast, typeParameter, typeWithExtension */
