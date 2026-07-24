// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-85169
@file:MustUseReturnValues

fun interface Foo {
    fun method()
}

fun Foo(arg: () -> Unit): Foo = TODO()

fun main() {
    // Object literal with a supertype: only `object : Foo` is highlighted, not the body.
    <!RETURN_VALUE_NOT_USED!>object : Foo<!> {
        override fun method() {}
    }

    // Object literal without a supertype: only the `object` keyword is highlighted.
    <!RETURN_VALUE_NOT_USED!>object<!> {
        val x = 1
    }

    // SAM-conversion call: highlighting falls on the callee, as before.
    <!RETURN_VALUE_NOT_USED!>Foo<!> {}
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, anonymousObjectExpression, funInterface, functionDeclaration,
functionalType, integerLiteral, interfaceDeclaration, lambdaLiteral, override, propertyDeclaration */
