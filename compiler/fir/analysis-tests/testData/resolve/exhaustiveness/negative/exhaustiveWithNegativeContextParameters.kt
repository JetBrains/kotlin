// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +DataFlowBasedExhaustiveness, +ContextParameters, +ContextSensitiveResolutionUsingExpectedType
// WITH_STDLIB

enum class MyEnum { A, B, C }

sealed interface MySealedInterface {
    object A : MySealedInterface
    object B : MySealedInterface
    object C : MySealedInterface
}

fun <T> genericEnum(e: T): Int where T : MyEnum {
    if (e == MyEnum.A) return 1
    return when (e) {
        MyEnum.B -> 2
        MyEnum.C -> 3
    }
}

context(x: MySealedInterface)
fun testContext(): Int {
    if (x == MySealedInterface.A) return 0
    return <!WHEN_ON_SEALED_GEEN_ELSE!>when (x) {
        MySealedInterface.B -> 2
        MySealedInterface.C -> 3
    }<!>
}

context(x: MySealedInterface?)
fun testNullableContext(): Int {
    if (x == null || x == MySealedInterface.B || x == MySealedInterface.C) return 0
    return <!WHEN_ON_SEALED_GEEN_ELSE!>when (x) {
        MySealedInterface.A -> 1
    }<!>
}

context(x: MySealedInterface?, flag: Boolean)
fun multiContextTest(): Int {
    requireNotNull(x)
    if (!flag || x == A ) return -1
    return <!WHEN_ON_SEALED_GEEN_ELSE!>when (x) {
        B -> 20
        C -> 30
    }<!>
}

context(x: MySealedInterface?, flag: Boolean)
fun multiContextTest2(): Int {
    requireNotNull(x)
    if (!flag || x == A ) return -1
    return <!WHEN_ON_SEALED_GEEN_ELSE!>when (x) {
        B, C -> 30
    }<!>
}

/* GENERATED_FIR_TAGS: disjunctionExpression, equalityExpression, functionDeclaration, functionDeclarationWithContext,
ifExpression, integerLiteral, interfaceDeclaration, nestedClass, nullableType, objectDeclaration, sealed, smartcast,
typeConstraint, typeParameter, whenExpression, whenWithSubject */
