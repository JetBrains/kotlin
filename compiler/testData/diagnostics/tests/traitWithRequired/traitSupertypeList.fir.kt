open class bar()

interface Foo() : bar(), bar, bar {
}

interface Foo2 : bar, Foo {
}

open class Foo1() : bar(), bar, Foo, <!UNRESOLVED_REFERENCE!>Foo<!>() {}
open class Foo12 : bar(), bar {}