// LL_FIR_DIVERGENCE
// Checkers are run with Common session in Analysis API, so they can't see actualized declarations
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB
// OPT_IN: kotlin.ExperimentalValueClassesApi
// MODULE: common

@WillBecomeValue
expect sealed class OverridingBase()

@WillBecomeValue
class OfOverridingBase(val x: Int) : OverridingBase()

@WillBecomeValue
expect sealed class NotOverridingBase()

@WillBecomeValue
class OfNotOverridingBase(val x: Int) : NotOverridingBase()

// MODULE: platform()()(common)

@WillBecomeValue
actual sealed class OverridingBase actual constructor() {
    override fun equals(other: Any?): Boolean = other is OverridingBase
    override fun hashCode(): Int = 0
    override fun toString(): String = "OverridingBase"
}

@WillBecomeValue
actual sealed class NotOverridingBase actual constructor()

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, integerLiteral, isExpression, nullableType,
operator, override, primaryConstructor, propertyDeclaration, sealed, stringLiteral */
