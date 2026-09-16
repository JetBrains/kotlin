// RUN_PIPELINE_TILL: CODEGEN

class X {
    fun <T> foo(): T = TODO()
}

fun test(x: X?) {
    val y = x?.foo() as Int
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, localProperty, nullableType,
propertyDeclaration, safeCall, typeParameter */
