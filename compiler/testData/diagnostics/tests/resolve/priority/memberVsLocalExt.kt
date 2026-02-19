// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE
// DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

class A {
    fun foo() = this
}


fun test(a: A) {
    fun A.<!EXTENSION_SHADOWED_BY_MEMBER!>foo<!>() = 4

    a.foo() checkType { _<A>() }

    with(a) {
        foo() checkType { _<A>() }
        this.foo() checkType { _<A>() }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, infix,
integerLiteral, lambdaLiteral, localFunction, nullableType, thisExpression, typeParameter, typeWithExtension */
