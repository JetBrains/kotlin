// RUN_PIPELINE_TILL: FRONTEND
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
    foo1(arrayOf())
    foo1(null)
    foo2()
    foo2(arrayOf())
    foo2(null)
    foo3()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, starProjection, typeParameter */
