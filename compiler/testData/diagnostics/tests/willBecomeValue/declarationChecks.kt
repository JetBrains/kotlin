// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

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
    <!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>var x: Int<!>,
    val y: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>A<!>,
    <!VALUE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER!>z: Int<!>,
) : <!VALUE_CLASS_CANNOT_EXTEND_IDENTITY_CLASSES!>IdentityBase<!>(), <!VALUE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION!>I<!> by IImpl() {
    override fun equals(other: Any?): Boolean = other is A && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "A"
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val field<!> = 3
}

@WillBecomeValue
object O {
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val field<!> = 3
}

@WillBecomeValue
class Ok(val x: Int, val y: String) : ValueBase(), I {
    override fun foo() {}
    override fun equals(other: Any?): Boolean = other is Ok && other.x == x && other.y == y
    override fun hashCode(): Int = 31 * x + y.hashCode()
    override fun toString(): String = "Ok"
}

@WillBecomeValue
class <!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS!>WithoutPrimaryConstructor<!> {
    constructor(x: Int)

    override fun equals(other: Any?): Boolean = other is WithoutPrimaryConstructor
    override fun hashCode(): Int = 0
    override fun toString(): String = "WithoutPrimaryConstructor"
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, classDeclaration, equalityExpression, functionDeclaration,
inheritanceDelegation, integerLiteral, interfaceDeclaration, isExpression, multiplicativeExpression, nullableType,
operator, override, primaryConstructor, propertyDeclaration, secondaryConstructor, smartcast */
