// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE_FEATURE_TOGGLED: StabilizeWillBecomeValueRestrictions

interface I {
    fun foo()
}

class IImpl : I {
    override fun foo() {}
}

open class IdentityBase

@WillBecomeValue
abstract class ValueBase

@WillBecomeValue
class A(
    <!WILL_BECOME_VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER_WARNING!>var x: Int<!>,
    val y: <!WILL_BECOME_VALUE_CLASS_CANNOT_BE_RECURSIVE_WARNING!>A<!>,
    <!WILL_BECOME_VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER_WARNING!>z: Int<!>,
) : <!WILL_BECOME_VALUE_CLASS_CANNOT_EXTEND_IDENTITY_CLASSES_WARNING!>IdentityBase<!>(), <!WILL_BECOME_VALUE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION_WARNING!>I<!> by IImpl() {
    override fun equals(other: Any?): Boolean = other is A && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "A"
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_WILL_BECOME_VALUE_CLASS_WARNING!>val field<!> = 3
}

@WillBecomeValue
object O {
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_WILL_BECOME_VALUE_CLASS_WARNING!>val field<!> = 3
    override fun toString(): String = "O"
}

@WillBecomeValue
class Ok(val x: Int, val y: String) : ValueBase(), I {
    override fun foo() {}
    override fun equals(other: Any?): Boolean = other is Ok && other.x == x && other.y == y
    override fun hashCode(): Int = 31 * x + y.hashCode()
    override fun toString(): String = "Ok"
}

@WillBecomeValue
class <!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_WILL_BECOME_VALUE_CLASS_WARNING!>WithoutPrimaryConstructor<!> {
    constructor(x: Int)

    override fun equals(other: Any?): Boolean = other is WithoutPrimaryConstructor
    override fun hashCode(): Int = 0
    override fun toString(): String = "WithoutPrimaryConstructor"
}

class Outer {
    @WillBecomeValue
    inner class <!WILL_BECOME_VALUE_CLASS_NOT_TOP_LEVEL_WARNING!>Inner<!>(val x: Int) {
        override fun equals(other: Any?): Boolean = other is Inner && other.x == x
        override fun hashCode(): Int = x
        override fun toString(): String = "Inner"
    }
}

fun local() {
    @WillBecomeValue
    data class <!WILL_BECOME_VALUE_CLASS_NOT_TOP_LEVEL_WARNING!>Local<!>(val x: Int)
}

@WillBecomeValue
class EmptyConstructor<!WILL_BECOME_VALUE_CLASS_EMPTY_CONSTRUCTOR_WARNING!>()<!> {
    override fun equals(other: Any?): Boolean = other is EmptyConstructor
    override fun hashCode(): Int = 0
    override fun toString(): String = "EmptyConstructor"
}

@WillBecomeValue
abstract class AbstractWithProperty(<!ABSTRACT_WILL_BECOME_VALUE_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER_WARNING!>val x: Int<!>)

@WillBecomeValue
sealed class SealedWithProperty(<!SEALED_WILL_BECOME_VALUE_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER_WARNING!>val x: Int<!>)

@WillBecomeValue
data class WithDelegatedProperty(val x: Int) {
    val y by <!DELEGATED_PROPERTY_INSIDE_WILL_BECOME_VALUE_CLASS_WARNING!>lazy { x }<!>
}

@WillBecomeValue
data class <!WILL_BECOME_VALUE_CLASS_CANNOT_BE_CLONEABLE_WARNING!>WithCloneable<!>(val x: Int) : Cloneable

@WillBecomeValue
data class RecursiveViaTypeParameter<T : RecursiveViaTypeParameter<T>>(val x: <!WILL_BECOME_VALUE_CLASS_CANNOT_BE_RECURSIVE_VIA_TYPE_PARAMETERS_WARNING!>T<!>)

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, classDeclaration, data, equalityExpression,
functionDeclaration, inheritanceDelegation, inner, integerLiteral, interfaceDeclaration, isExpression, lambdaLiteral,
localClass, multiplicativeExpression, nullableType, objectDeclaration, operator, override, primaryConstructor,
propertyDeclaration, propertyDelegate, sealed, secondaryConstructor, smartcast, stringLiteral, typeConstraint,
typeParameter */
