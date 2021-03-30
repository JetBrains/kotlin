open class bar()

interface <error descr="[CONSTRUCTOR_IN_INTERFACE] An interface may not have a constructor">Foo()</error> : <error descr="[INTERFACE_WITH_SUPERCLASS] An interface cannot inherit from a class"><error descr="[SUPERTYPE_INITIALIZED_IN_INTERFACE] Interfaces cannot initialize supertypes">bar</error></error>(), bar, bar {
}

interface Foo2 : <error descr="[INTERFACE_WITH_SUPERCLASS] An interface cannot inherit from a class">bar</error>, Foo {
}

open class Foo1() : bar(), bar, Foo, <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: <init>">Foo</error>() {}
open class Foo12 : bar(), bar {}
