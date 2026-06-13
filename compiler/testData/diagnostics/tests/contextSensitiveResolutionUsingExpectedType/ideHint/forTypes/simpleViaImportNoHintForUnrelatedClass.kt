// RUN_PIPELINE_TILL: FRONTEND
// IDE_MODE

// FILE: test.kt
package test

sealed class A {
    class Z : A()
}

// FILE: other.kt
package other

class X

// FILE: main.kt
import test.A
import other.X

fun foo(a: A) {
    // No hint: simple-name `X` is resolved through ExplicitImport to other.X,
    // but CSR over the parent chain of `a` (sealed class A) has no `X` to find,
    // so the imported symbol differs from any CSR result.
    if (<!IMPOSSIBLE_IS_CHECK_ERROR!>a is X<!>) {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, isExpression, nestedClass, sealed */
