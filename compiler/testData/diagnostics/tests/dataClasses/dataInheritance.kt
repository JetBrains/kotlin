interface Allowed

open class NotAllowed

abstract data class Base(val x: Int)

class Derived: Base(42)

<!DATA_CLASS_OVERRIDE_CONFLICT!>data<!> class Nasty(x: Int, val y: Int): <!DATA_CLASS_CANNOT_HAVE_CLASS_SUPERTYPES!>Base<!>(x)

data class Complex(val y: Int): Allowed, <!DATA_CLASS_CANNOT_HAVE_CLASS_SUPERTYPES!>NotAllowed<!>()