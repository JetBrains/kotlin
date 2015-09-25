interface Allowed

open class NotAllowed

<!DEPRECATED_MODIFIER_PAIR!>abstract<!> <!DEPRECATED_MODIFIER_PAIR!>data<!> class Base(val x: Int)

class Derived: Base(42)

<!DATA_CLASS_OVERRIDE_CONFLICT!>data<!> class Nasty(val z: Int, val y: Int): <!DATA_CLASS_CANNOT_HAVE_CLASS_SUPERTYPES!>Base<!>(z)

data class Complex(val y: Int): Allowed, <!DATA_CLASS_CANNOT_HAVE_CLASS_SUPERTYPES!>NotAllowed<!>()