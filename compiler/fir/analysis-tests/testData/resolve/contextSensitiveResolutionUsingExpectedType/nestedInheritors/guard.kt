// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// ISSUE: KT-75977

sealed class SealedClass {
    open class SealedInheritor(val prop: String = "1"): SealedClass()
    class IndirectSealedInheritor: SealedInheritor()
}

fun testIsInWhenGuardSealedClass(instance: SealedClass): String {
    return <!WHEN_ON_SEALED_EEN_EN_ELSE!>when (instance) {
        is SealedInheritor if instance is IndirectSealedInheritor -> instance.prop
        else -> "100"
    }<!>
}

open class OpenClass {
    open class SealedInheritorOfOpenClass(val prop: String = "1"): OpenClass()
    class IndirectSealedInheritorOfOpenClass: SealedInheritorOfOpenClass()
}

fun testIsInWhenGuardOpenClass(instance: OpenClass): String {
    return when (instance) {
        is <!UNRESOLVED_REFERENCE!>SealedInheritorOfOpenClass<!> if instance is <!UNRESOLVED_REFERENCE!>IndirectSealedInheritorOfOpenClass<!> -> instance.<!UNRESOLVED_REFERENCE!>prop<!>
        else -> "100"
    }
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, functionDeclaration, guardCondition, isExpression, nestedClass,
primaryConstructor, propertyDeclaration, sealed, smartcast, stringLiteral, whenExpression, whenWithSubject */
