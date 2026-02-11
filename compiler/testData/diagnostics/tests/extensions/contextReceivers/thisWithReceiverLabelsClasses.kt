// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED, -CONTEXT_CLASS_OR_CONSTRUCTOR
// LANGUAGE: +ContextReceivers, -ContextParameters

class A {
    val x = 1
}

context(A) class B {
    val prop = x + this@A.x

    fun f() = x + this@A.x
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration,
thisExpression */
