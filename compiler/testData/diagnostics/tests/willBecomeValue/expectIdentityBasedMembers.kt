// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects, +AllowExpectValueClassesWithNoPrimaryConstructor
// WITH_STDLIB
// OPT_IN: kotlin.ExperimentalValueClassesApi
// MODULE: common

@WillBecomeValue
expect class Overridden

@WillBecomeValue
expect object OverriddenObject

@WillBecomeValue
expect class NotOverridden

// MODULE: platform()()(common)

@WillBecomeValue
actual class Overridden(val value: Int) {
    override fun equals(other: Any?): Boolean = other is Overridden && other.value == value
    override fun hashCode(): Int = value
    override fun toString(): String = "Overridden($value)"
}

@WillBecomeValue
actual object OverriddenObject {
    override fun toString(): String = "OverriddenObject"
}

@WillBecomeValue
actual <!IDENTITY_BASED_MEMBER_IN_WILL_BECOME_VALUE_CLASS, IDENTITY_BASED_MEMBER_IN_WILL_BECOME_VALUE_CLASS, IDENTITY_BASED_MEMBER_IN_WILL_BECOME_VALUE_CLASS!>class NotOverridden<!>(val value: Int)

/* GENERATED_FIR_TAGS: actual, andExpression, classDeclaration, equalityExpression, expect, functionDeclaration,
isExpression, nullableType, objectDeclaration, operator, override, primaryConstructor, propertyDeclaration, smartcast,
stringLiteral */
