// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

interface A {
    fun foo(): CharSequence
}

interface B {
    fun foo(): String?
}

fun test(c: Any) {
    if (c is B && c is A) {
        c.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>()
    }
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
ifExpression, infix, interfaceDeclaration, intersectionType, isExpression, nullableType, smartcast, typeParameter,
typeWithExtension */
