interface IFoo

typealias Test = IFoo

val testAsFunction = <!INTERFACE_AS_FUNCTION!>Test<!>()
val testAsValue = <!NO_COMPANION_OBJECT!>Test<!>
