open class bar()

interface <error descr="[CONSTRUCTOR_IN_INTERFACE] An interface may not have a constructor">Foo()</error> : <error descr="[INTERFACE_WITH_SUPERCLASS] An interface cannot inherit from a class"><error descr="[SUPERTYPE_INITIALIZED_IN_INTERFACE] Interfaces cannot initialize supertypes">bar</error></error>(), <error descr="[MANY_CLASSES_IN_SUPERTYPE_LIST] Only one class may appear in a supertype list"><error descr="[SUPERTYPE_APPEARS_TWICE] A supertype appears twice">bar</error></error>, <error descr="[MANY_CLASSES_IN_SUPERTYPE_LIST] Only one class may appear in a supertype list"><error descr="[SUPERTYPE_APPEARS_TWICE] A supertype appears twice">bar</error></error> {
}

interface Foo2 : <error descr="[INTERFACE_WITH_SUPERCLASS] An interface cannot inherit from a class">bar</error>, Foo {
}

open class Foo1() : bar(), <error descr="[MANY_CLASSES_IN_SUPERTYPE_LIST] Only one class may appear in a supertype list"><error descr="[SUPERTYPE_APPEARS_TWICE] A supertype appears twice">bar</error></error>, Foo, <error descr="[SUPERTYPE_APPEARS_TWICE] A supertype appears twice"><error descr="[UNRESOLVED_REFERENCE] Unresolved reference: <init>">Foo</error></error>() {}
open class Foo12 : bar(), <error descr="[MANY_CLASSES_IN_SUPERTYPE_LIST] Only one class may appear in a supertype list"><error descr="[SUPERTYPE_APPEARS_TWICE] A supertype appears twice">bar</error></error> {}
