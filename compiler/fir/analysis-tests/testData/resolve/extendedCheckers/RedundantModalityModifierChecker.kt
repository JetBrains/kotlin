// LANGUAGE: +MultiPlatformProjects

object O {
    fun foo() {}
}

// Interface
interface Interface {
    // Questionable cuz compiler reports warning here in FE 1.0
    <!REDUNDANT_MODALITY_MODIFIER!>open<!> val gav: Int
        get() = 42
    // Redundant
    <!REDUNDANT_MODALITY_MODIFIER!>abstract<!> fun foo()
    // error
    <!PRIVATE_FUNCTION_WITH_NO_BODY!>private<!> <!WRONG_MODIFIER_CONTAINING_DECLARATION!>final<!> fun bar()

    <!REDUNDANT_MODALITY_MODIFIER, REDUNDANT_OPEN_IN_INTERFACE!>open<!> fun loo()
    <!REDUNDANT_MODALITY_MODIFIER!>open<!> fun goo() {}
    <!REDUNDANT_MODALITY_MODIFIER!>abstract<!> fun tar()

    // error
    <!ABSTRACT_FUNCTION_WITH_BODY!>abstract<!> fun too() {}
}
interface B {
    <!REDUNDANT_MODALITY_MODIFIER!>abstract<!> var bar: Unit
    <!REDUNDANT_MODALITY_MODIFIER!>abstract<!> fun foo()
}
interface Foo

expect abstract class AbstractClass : Foo {
    abstract <!NOTHING_TO_OVERRIDE!>override<!> fun foo()

    abstract fun bar()

    abstract val baz: Int
}


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
    override <!NON_FINAL_MEMBER_IN_FINAL_CLASS!>open<!> val gav = 13
}
// Open
open class OpenDerived : Base() {
    // Final
    override final fun bar() {}
    // Redundant open
    override <!REDUNDANT_MODALITY_MODIFIER!>open<!> val gav = 13

    private <!REDUNDANT_MODALITY_MODIFIER!>final<!> fun fan() {}
}
// Redundant final
<!REDUNDANT_MODALITY_MODIFIER!>final<!> class Final
// Derived interface
interface Derived : Interface {
    // Redundant
    override <!REDUNDANT_MODALITY_MODIFIER!>open<!> fun foo() {}
    // error
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>final<!> class Nested
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
<!REDUNDANT_MODALITY_MODIFIER!>abstract<!> interface AbstractInterface
// Redundant final object
<!REDUNDANT_MODALITY_MODIFIER!>final<!> object FinalObject
// Open interface
<!REDUNDANT_MODALITY_MODIFIER, REDUNDANT_MODIFIER_FOR_TARGET!>open<!> interface OpenInterface

class FinalDerived2(override <!REDUNDANT_MODALITY_MODIFIER!>final<!> val gav: Int) : Base() {
    override fun bar() {}
}
