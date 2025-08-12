// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-79866
// LANGUAGE: -ForbidUsingExpressionTypesWithInaccessibleContent
// MODULE: bar
// FILE: Bar.kt

package test

class Bar<T>

class Bar2

// MODULE: foo(bar)
// FILE: Foo.kt

package test

fun foo1(array: Array<Bar<*>>? = null) {}

fun foo2(array: Array<Bar2>? = null) {}

fun foo3(bar: Bar<*>? = null) {}

// MODULE: test(foo)
// FILE: Test.kt

package test

fun main() {
    foo1()
    <!MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE!>foo1<!>(<!MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE!>arrayOf<!>())
    <!MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE!>foo1<!>(null)
    foo2()
    <!MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE!>foo2<!>(<!MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE!>arrayOf<!>())
    <!MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE!>foo2<!>(null)
    foo3()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, starProjection, typeParameter */
