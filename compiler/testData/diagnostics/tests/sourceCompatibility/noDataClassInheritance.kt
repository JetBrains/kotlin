// !LANGUAGE: -DataClassInheritance

interface Allowed

open class NotAllowed

<!INCOMPATIBLE_MODIFIERS!>abstract<!> <!INCOMPATIBLE_MODIFIERS!>data<!> class Base(val x: Int)

class Derived: Base(42)

<!DATA_CLASS_OVERRIDE_CONFLICT!>data<!> class Nasty(val z: Int, val y: Int): <!DATA_CLASS_CANNOT_HAVE_CLASS_SUPERTYPES!>Base<!>(z)

data class Complex(val y: Int): Allowed, <!DATA_CLASS_CANNOT_HAVE_CLASS_SUPERTYPES!>NotAllowed<!>()



interface AbstractEqualsHashCodeToString {
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String
}

data <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class ImplInterface<!>(val s: String) : AbstractEqualsHashCodeToString
