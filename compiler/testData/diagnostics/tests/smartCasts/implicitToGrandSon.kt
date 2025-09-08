// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
open class A {
    open fun foo() = "FAIL"

    fun bar() = if (this is C) foo() else "FAIL"
}

open class B : A()

open class C : B() {
    override fun foo() = "OK"
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, isExpression, override, smartcast,
stringLiteral, thisExpression */
