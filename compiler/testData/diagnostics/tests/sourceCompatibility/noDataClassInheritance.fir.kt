// !LANGUAGE: -DataClassInheritance

interface Allowed

open class NotAllowed

<!INCOMPATIBLE_MODIFIERS!>abstract<!> <!INCOMPATIBLE_MODIFIERS!>data<!> class Base(val x: Int)

class Derived: Base(42)

<!DATA_CLASS_OVERRIDE_CONFLICT!>data<!> class Nasty(val z: Int, val y: Int): Base(z)

data class Complex(val y: Int): Allowed, NotAllowed()



interface AbstractEqualsHashCodeToString {
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String
}

data class ImplInterface(val s: String) : AbstractEqualsHashCodeToString
