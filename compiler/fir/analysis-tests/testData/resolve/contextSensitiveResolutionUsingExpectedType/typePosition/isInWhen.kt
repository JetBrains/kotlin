// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75061
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

sealed class SealedClass {
    open class SealedInheritor1(val prop1: String = "1"): SealedClass()
    class SealedInheritor2(val prop2: Int = 2): SealedClass()

    sealed class SealedSealedInheritor1(val prop3: Boolean = true): SealedClass()

    class IndirectSealedInheritor: SealedInheritor1()

    inner class Inner(val innerProp: String = "inner prop"): SealedClass()

}

fun testIsInWhen(instance: SealedClass): String {
    return when {
        instance is SealedInheritor1 -> instance.prop1
        instance is Inner -> instance.innerProp
        instance is IndirectSealedInheritor -> instance.prop1
        instance !is SealedInheritor2 -> "100"
        <!IMPOSSIBLE_IS_CHECK_ERROR!>instance !is SealedSealedInheritor1<!> -> "100"
        else -> "100"
    }
}

fun testIsInWhenWithSubject(instance: SealedClass): String {
    return when (instance) {
        is SealedInheritor1 -> instance.prop1
        is Inner -> instance.innerProp
        is IndirectSealedInheritor -> instance.prop1
        !is SealedInheritor2 -> "100"
        <!IMPOSSIBLE_IS_CHECK_ERROR!>!is SealedSealedInheritor1<!> -> "100"
        else -> "100"
    }
}

fun testIsInWhenGuard(instance: SealedClass): String {
    return <!RETURN_TYPE_MISMATCH!>when (instance) {
        is SealedInheritor1 if instance is IndirectSealedInheritor -> instance.prop1
        // KT-75977
        !is SealedInheritor1 if instance is SealedInheritor2 -> instance.prop2
        !is SealedInheritor2 if instance is SealedInheritor1 -> instance.prop1
        !is SealedInheritor1 if instance is SealedSealedInheritor1 -> instance.prop3
        else -> "100"
    }<!>
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, functionDeclaration, guardCondition, inner, integerLiteral,
isExpression, nestedClass, primaryConstructor, propertyDeclaration, sealed, smartcast, stringLiteral, whenExpression,
whenWithSubject */
