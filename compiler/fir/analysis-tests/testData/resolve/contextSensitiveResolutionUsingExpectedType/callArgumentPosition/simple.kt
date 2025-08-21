// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75315
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class MyEnum {
    X, Y
}

fun foo(a: MyEnum) {}

fun main() {

    val L = MyEnum.X

    foo(X)
    foo(L)
    foo(<!UNRESOLVED_REFERENCE!>UNRESOLVED<!>)
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, localProperty, propertyDeclaration */
