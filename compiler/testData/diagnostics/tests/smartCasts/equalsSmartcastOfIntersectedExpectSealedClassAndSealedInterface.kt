// LANGUAGE: +MultiPlatformProjects
// RUN_PIPELINE_TILL: FRONTEND

// MODULE: common
// FILE: file1.kt
package example

sealed interface A {
    fun a() = Unit
}

expect sealed class B() {
    open fun b()
}

class C : B(), A
class D : A

fun useSite(x: Any?, y: Any?) {
    if (x is A && x is B && x == y) {
        // smartcast is impossible because we can't be sure that all inheritors of `A` have trivial `equals`:
        // some may inherit it from `B` which is expect
        y.<!UNRESOLVED_REFERENCE!>b<!>()
        y.<!UNRESOLVED_REFERENCE!>a<!>()
    }
}

// MODULE: jvm()()(common)
// FILE: file2.kt
package example

actual sealed class B {
    actual open fun b() = Unit
}

/* GENERATED_FIR_TAGS: actual, annotationDeclaration, classDeclaration, classReference, expect, functionDeclaration,
nullableType, operator, override, primaryConstructor, propertyDeclaration, starProjection, typeAliasDeclaration */
