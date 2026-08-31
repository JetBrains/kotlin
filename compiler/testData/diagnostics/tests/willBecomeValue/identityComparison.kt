// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// DIAGNOSTICS: -UNUSED_VARIABLE

@WillBecomeValue
class Wrapper(val x: Int) {
    override fun equals(other: Any?): Boolean = other is Wrapper && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "Wrapper($x)"

    fun isSame(other: Wrapper): Boolean = <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS_ERROR!>this<!> === <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS_ERROR!>other<!>

    fun isSameAsNull(): Boolean = <!SENSELESS_COMPARISON!>this === null<!>
}

class NotAValue

fun test(a: Wrapper, b: Wrapper, nullable: Wrapper?, other: NotAValue) {
    val a1 = <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>a<!> === <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>b<!>
    val a2 = <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>a<!> !== <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>b<!>
    val a3 = <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>a<!> === <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>nullable<!>
    val a4 = nullable === null
    val a5 = other === other

    val a6 = a as Any === b as Any
    val a7 = <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>a<!> === Any()

    val a8 = <!EQUALITY_NOT_APPLICABLE!><!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>a<!> === other<!>
}

class Nested {
    class Deeper {
        fun test(a: Wrapper, b: Wrapper) = <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>a<!> === <!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>b<!>
    }
}

/* GENERATED_FIR_TAGS: andExpression, asExpression, classDeclaration, equalityExpression, functionDeclaration,
isExpression, localProperty, nestedClass, nullableType, operator, override, primaryConstructor, propertyDeclaration,
smartcast, thisExpression */
