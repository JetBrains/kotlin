// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

interface A {
    fun <T, E> foo(): E
}

interface B {
    fun <Q, W> foo(): W
}

fun test(c: Any) {
    if (c is B && c is A) {
        <!DEBUG_INFO_SMARTCAST!>c<!>.foo<String, Int>().checkType { _<Int>() }
    }
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
ifExpression, infix, interfaceDeclaration, intersectionType, isExpression, lambdaLiteral, nullableType, smartcast,
typeParameter, typeWithExtension */
