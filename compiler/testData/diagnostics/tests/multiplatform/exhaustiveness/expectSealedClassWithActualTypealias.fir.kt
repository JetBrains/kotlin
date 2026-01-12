// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-45796
// SKIP_TXT

// MODULE: m1-common
expect sealed class SealedClass()

class Derived1 : SealedClass()

// MODULE: m1-jvm()()(m1-common)
actual typealias SealedClass = MySealedClass

sealed class MySealedClass

class Derived2 : SealedClass()
class Derived3 : MySealedClass()

fun whenForSealed(s: SealedClass): Int {
    return <!WHEN_ON_SEALED!>when (s) { // Should be OK
        is Derived1 -> 1
        is Derived2 -> 2
        is Derived3 -> 3
    }<!>
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, integerLiteral, isExpression,
primaryConstructor, sealed, smartcast, typeAliasDeclaration, whenExpression, whenWithSubject */
