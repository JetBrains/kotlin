// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-45796

// MODULE: m1-common
expect sealed class SealedClass() {
    class Nested : SealedClass {
        class NestedDeeper : SealedClass
    }
}

fun whenForExpectSealed(s: SealedClass): Int {
    return <!NO_ELSE_IN_WHEN!>when<!> (s) { // should be error, because actual sealed class may add more implementations
        is SealedClass.Nested.NestedDeeper -> 7
        is SealedClass.Nested -> 8
    }
}

// MODULE: m1-jvm()()(m1-common)
actual sealed class SealedClass {
    actual class Nested : SealedClass() {
        actual class NestedDeeper : SealedClass()
    }
}

fun whenForSealed(s: SealedClass): Int {
    return <!WHEN_ON_SEALED_GEEN_ELSE!>when (s) { // Should be OK
        is SealedClass.Nested.NestedDeeper -> 7
        is SealedClass.Nested -> 8
    }<!>
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, integerLiteral, isExpression, nestedClass,
primaryConstructor, sealed, smartcast, whenExpression, whenWithSubject */
