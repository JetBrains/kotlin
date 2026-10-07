// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// OPT_IN: kotlin.ExperimentalValueClassesApi
// RENDER_DIAGNOSTICS_FULL_TEXT

@WillBecomeValue
class Wrapper(val x: Int) {
    override fun equals(other: Any?): Boolean = other is Wrapper && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "Wrapper($x)"
}

fun <T : <!FINAL_UPPER_BOUND!>Wrapper<!>> bounded(a: T, b: T) = <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>a<!> === <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>b<!>

fun <T : Wrapper?> nullableBound(a: T, b: T) = <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>a<!> === <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>b<!>

fun <T> smartCast(a: T, b: Wrapper) = if (a is Wrapper) <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>a<!> === <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>b<!> else false

fun <T : <!FINAL_UPPER_BOUND!>Wrapper<!>> lock(t: T) = synchronized(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>t<!>) {}

class Box<T : <!FINAL_UPPER_BOUND!>Wrapper<!>>(val t: T) {
    fun isSame(other: Box<T>) = <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>t<!> === <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>other.t<!>
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, equalityExpression, functionDeclaration, ifExpression,
intersectionType, isExpression, lambdaLiteral, nullableType, operator, override, primaryConstructor, propertyDeclaration,
smartcast, stringLiteral, typeConstraint, typeParameter */
