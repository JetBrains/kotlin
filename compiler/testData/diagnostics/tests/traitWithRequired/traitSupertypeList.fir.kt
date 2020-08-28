open class bar()

interface Foo() : <!INTERFACE_WITH_SUPERCLASS!>bar<!>(), bar, bar {
}

interface Foo2 : <!INTERFACE_WITH_SUPERCLASS!>bar<!>, Foo {
}

open class Foo1() : bar(), bar, Foo, <!UNRESOLVED_REFERENCE!>Foo<!>() {}
open class Foo12 : bar(), bar {}