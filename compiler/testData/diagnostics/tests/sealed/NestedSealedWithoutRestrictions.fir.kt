// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-13495
// LANGUAGE: +AllowSealedInheritorsInDifferentFilesOfSamePackage
// DIAGNOSTICS: -UNUSED_VARIABLE

// FILE: base.kt

package foo

class Container {
    sealed class Base
}

// FILE: a.kt

package foo

class A : Container.Base()

// FILE: b.kt

package foo

class BContainer {
    class B : Container.Base()

    inner class C : Container.Base()
}

// FILE: test.kt

package foo

fun test(base: Container.Base) {
    val x = <!WHEN_ON_SEALED_GEEN_ELSE!>when (base) {
        is A -> 1
        is BContainer.B -> 2
        is BContainer.C -> 3
    }<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inner, integerLiteral, isExpression, localProperty,
nestedClass, propertyDeclaration, sealed, smartcast, whenExpression, whenWithSubject */
