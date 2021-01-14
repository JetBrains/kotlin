object O {
    fun foo() {}
}

// Interface
interface Interface {
    <!REDUNDANT_MODALITY_MODIFIER{LT}!>// Questionable cuz compiler reports warning here in FE 1.0
    <!REDUNDANT_MODALITY_MODIFIER{PSI}!>open<!> val gav: Int
        get() = 42<!>
    <!REDUNDANT_MODALITY_MODIFIER{LT}!>// Redundant
    <!REDUNDANT_MODALITY_MODIFIER{PSI}!>abstract<!> fun foo()<!>
    <!PRIVATE_FUNCTION_WITH_NO_BODY{LT}!>// error
    <!PRIVATE_FUNCTION_WITH_NO_BODY{PSI}!>private<!> final fun bar()<!>

    <!REDUNDANT_MODALITY_MODIFIER{LT}!><!REDUNDANT_MODALITY_MODIFIER{PSI}!>open<!> fun goo() {}<!>
    <!REDUNDANT_MODALITY_MODIFIER{LT}!><!REDUNDANT_MODALITY_MODIFIER{PSI}!>abstract<!> fun tar()<!>

    <!ABSTRACT_FUNCTION_WITH_BODY{LT}!>// error
    <!ABSTRACT_FUNCTION_WITH_BODY{PSI}!>abstract<!> fun too() {}<!>
}
interface B {
    <!REDUNDANT_MODALITY_MODIFIER{LT}!><!REDUNDANT_MODALITY_MODIFIER{PSI}!>abstract<!> var bar: Unit<!>
    <!REDUNDANT_MODALITY_MODIFIER{LT}!><!REDUNDANT_MODALITY_MODIFIER{PSI}!>abstract<!> fun foo()<!>
}
interface Foo

expect abstract class AbstractClass : Foo {
    abstract override fun foo()

    abstract fun bar()

    abstract val baz: Int
}


// Abstract
abstract class Base {
    <!REDUNDANT_MODALITY_MODIFIER{LT}!>// Redundant final
    <!REDUNDANT_MODALITY_MODIFIER{PSI}!>final<!> fun foo() {}<!>
    // Abstract
    abstract fun bar()
    // Open
    open val gav = 42
}

class FinalDerived : Base() {
    <!REDUNDANT_MODALITY_MODIFIER{LT}!>// Redundant final
    override <!REDUNDANT_MODALITY_MODIFIER{PSI}!>final<!> fun bar() {}<!>
    // Non-final member in final class
    override open val gav = 13
}
// Open
open class OpenDerived : Base() {
    // Final
    override final fun bar() {}
    <!REDUNDANT_MODALITY_MODIFIER{LT}!>// Redundant open
    override <!REDUNDANT_MODALITY_MODIFIER{PSI}!>open<!> val gav = 13<!>
}
<!REDUNDANT_MODALITY_MODIFIER{LT}!>// Redundant final
<!REDUNDANT_MODALITY_MODIFIER{PSI}!>final<!> class Final<!>
// Derived interface
interface Derived : Interface {
    <!REDUNDANT_MODALITY_MODIFIER{LT}!>// Redundant
    override <!REDUNDANT_MODALITY_MODIFIER{PSI}!>open<!> fun foo() {}<!>
    // error
    final class Nested
}
// Derived abstract class
abstract class AbstractDerived1(override final val gav: Int) : Interface {
    <!REDUNDANT_MODALITY_MODIFIER{LT}!>// Redundant
    override <!REDUNDANT_MODALITY_MODIFIER{PSI}!>open<!> fun foo() {}<!>
}
// Derived abstract class
abstract class AbstractDerived2 : Interface {
    // Final
    override final fun foo() {}
    <!REDUNDANT_MODALITY_MODIFIER{LT}!>// Redundant
    override <!REDUNDANT_MODALITY_MODIFIER{PSI}!>open<!> val gav = 13<!>
}
// Redundant abstract interface
abstract interface AbstractInterface
<!REDUNDANT_MODALITY_MODIFIER{LT}!>// Redundant final object
<!REDUNDANT_MODALITY_MODIFIER{PSI}!>final<!> object FinalObject<!>
// Open interface
open interface OpenInterface
