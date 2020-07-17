// Abstract
abstract class Base {
    // Redundant final
    <!REDUNDANT_MODALITY_MODIFIER!>final<!> fun foo() {}
    // Abstract
    abstract fun bar()
    // Open
    open val gav = 42
}

class FinalDerived : Base() {
    // Redundant final
    override <!REDUNDANT_MODALITY_MODIFIER!>final<!> fun bar() {}
    // Non-final member in final class
    override open val gav = 13
}
// Open
open class OpenDerived : Base() {
    // Final
    override final fun bar() {}
    // Redundant open
    override <!REDUNDANT_MODALITY_MODIFIER!>open<!> val gav = 13
}
// Redundant final
<!REDUNDANT_MODALITY_MODIFIER!>final<!> class Final
// Interface
interface Interface {
    // Redundant
    <!REDUNDANT_MODALITY_MODIFIER!>abstract<!> fun foo()
    // Redundant
    private <!REDUNDANT_MODALITY_MODIFIER!>final<!> fun bar() {}
    // Redundant
    <!REDUNDANT_MODALITY_MODIFIER!>open<!> val gav: Int
        get() = 42
}
// Derived interface
interface Derived : Interface {
    // Redundant
    override <!REDUNDANT_MODALITY_MODIFIER!>open<!> fun foo() {}
    // Redundant
    <!REDUNDANT_MODALITY_MODIFIER!>final<!> class Nested
}
// Derived abstract class
abstract class AbstractDerived1(override final val gav: Int) : Interface {
    // Redundant
    override <!REDUNDANT_MODALITY_MODIFIER!>open<!> fun foo() {}
}
// Derived abstract class
abstract class AbstractDerived2 : Interface {
    // Final
    override final fun foo() {}
    // Redundant
    override <!REDUNDANT_MODALITY_MODIFIER!>open<!> val gav = 13
}
// Redundant abstract interface
abstract interface AbstractInterface
// Redundant final object
<!REDUNDANT_MODALITY_MODIFIER!>final<!> object FinalObject
// Open interface
open interface OpenInterface
