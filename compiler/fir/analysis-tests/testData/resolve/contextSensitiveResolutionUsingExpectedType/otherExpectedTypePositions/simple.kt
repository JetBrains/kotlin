// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75316
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class MyEnum {
    X, Y
}

fun foo(a: MyEnum) {}

fun main() {

    val L = MyEnum.X

    val t1: MyEnum = X
    val t2: MyEnum = L
    val t3: MyEnum = <!UNRESOLVED_REFERENCE!>UNRESOLVED<!>
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, localProperty, propertyDeclaration */
