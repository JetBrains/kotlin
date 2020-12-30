open class bar()

interface <!CONSTRUCTOR_IN_INTERFACE!>Foo()<!> : <!INTERFACE_WITH_SUPERCLASS, SUPERTYPE_INITIALIZED_IN_INTERFACE!>bar<!>(), bar, bar {
}

interface Foo2 : <!INTERFACE_WITH_SUPERCLASS!>bar<!>, Foo {
}

open class Foo1() : bar(), bar, Foo, <!UNRESOLVED_REFERENCE!>Foo<!>() {}
open class Foo12 : bar(), bar {}