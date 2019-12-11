interface IFoo

typealias Test = IFoo

val testAsFunction = <!UNRESOLVED_REFERENCE!>Test<!>()
val testAsValue = Test