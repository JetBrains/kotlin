// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

// 'Any.equals'/'Any.hashCode' compare and hash by identity, and 'Any.toString' renders the identity hash code, so all
// three change behavior once the class becomes a value class and must be overridden.
@WillBecomeValue
<!IDENTITY_BASED_MEMBER_IN_WILL_BECOME_VALUE_CLASS, IDENTITY_BASED_MEMBER_IN_WILL_BECOME_VALUE_CLASS, IDENTITY_BASED_MEMBER_IN_WILL_BECOME_VALUE_CLASS!>class NoOverrides<!>(val x: Int)

@WillBecomeValue
<!IDENTITY_BASED_MEMBER_IN_WILL_BECOME_VALUE_CLASS, IDENTITY_BASED_MEMBER_IN_WILL_BECOME_VALUE_CLASS!>class OnlyEquals<!>(val x: Int) {
    override fun equals(other: Any?): Boolean = other is OnlyEquals && other.x == x
}

@WillBecomeValue
<!IDENTITY_BASED_MEMBER_IN_WILL_BECOME_VALUE_CLASS, IDENTITY_BASED_MEMBER_IN_WILL_BECOME_VALUE_CLASS!>class OnlyHashCode<!>(val x: Int) {
    override fun hashCode(): Int = x
}

@WillBecomeValue
<!IDENTITY_BASED_MEMBER_IN_WILL_BECOME_VALUE_CLASS, IDENTITY_BASED_MEMBER_IN_WILL_BECOME_VALUE_CLASS!>class OnlyToString<!>(val x: Int) {
    override fun toString(): String = "OnlyToString($x)"
}

@WillBecomeValue
class AllOverridden(val x: Int) {
    override fun equals(other: Any?): Boolean = other is AllOverridden && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "AllOverridden($x)"
}

@WillBecomeValue
data class Generated(val x: Int)

@WillBecomeValue
abstract class StructuralBase {
    override fun equals(other: Any?): Boolean = other is StructuralBase
    override fun hashCode(): Int = 0
    override fun toString(): String = "StructuralBase"
}

@WillBecomeValue
class Inherited(val x: Int) : StructuralBase()

@WillBecomeValue
class DelegatingToSuper(val x: Int) {
    override fun equals(other: Any?): Boolean = super.equals(other)
    override fun hashCode(): Int = super.hashCode()
    override fun toString(): String = super.toString()
}

@WillBecomeValue
abstract class AbstractNoOverrides

@WillBecomeValue
<!IDENTITY_BASED_MEMBER_IN_WILL_BECOME_VALUE_CLASS, IDENTITY_BASED_MEMBER_IN_WILL_BECOME_VALUE_CLASS, IDENTITY_BASED_MEMBER_IN_WILL_BECOME_VALUE_CLASS!>class InheritedNoOverrides<!>(val x: Int) : AbstractNoOverrides()

@WillBecomeValue
sealed class SealedNoOverrides

@WillBecomeValue
object ObjectNoOverrides

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, data, equalityExpression, functionDeclaration, integerLiteral,
isExpression, nullableType, objectDeclaration, operator, override, primaryConstructor, propertyDeclaration, sealed,
smartcast, stringLiteral, superExpression */
