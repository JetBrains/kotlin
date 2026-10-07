// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// OPT_IN: kotlin.ExperimentalValueClassesApi

@WillBecomeValue
abstract class Base

@WillBecomeValue
class Child(val x: Int) : Base() {
    override fun equals(other: Any?): Boolean = other is Child && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "Child($x)"

    fun isSameBase(other: Base) = <!IDENTITY_SENSITIVE_OPERATION_INSIDE_WILL_BECOME_VALUE_CLASS!>this<!> === <!IDENTITY_SENSITIVE_OPERATION_INSIDE_WILL_BECOME_VALUE_CLASS!>other<!>
    fun areSameBases(a: Base, b: Base) = <!IDENTITY_SENSITIVE_OPERATION_INSIDE_WILL_BECOME_VALUE_CLASS!>a<!> === <!IDENTITY_SENSITIVE_OPERATION_INSIDE_WILL_BECOME_VALUE_CLASS!>b<!>
    fun areSameUnrelated(a: Unrelated, b: Unrelated) = <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>a<!> === <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>b<!>
}

@WillBecomeValue
class Unrelated(val x: Int) {
    override fun equals(other: Any?): Boolean = other is Unrelated && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "Unrelated($x)"
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, equalityExpression, functionDeclaration, isExpression,
nullableType, operator, override, primaryConstructor, propertyDeclaration, smartcast, stringLiteral, thisExpression */
