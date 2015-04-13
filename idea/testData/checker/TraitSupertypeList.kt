open class bar()

trait Foo<error descr="[CONSTRUCTOR_IN_TRAIT] A trait may not have a constructor">()</error> : <warning descr="[TRAIT_WITH_SUPERCLASS] Specifying a required base class for trait implementations is deprecated">bar</warning><error descr="[SUPERTYPE_INITIALIZED_IN_TRAIT] Traits cannot initialize supertypes">()</error>, <error descr="[MANY_CLASSES_IN_SUPERTYPE_LIST] Only one class may appear in a supertype list"><error descr="[SUPERTYPE_APPEARS_TWICE] A supertype appears twice">bar</error></error>, <error descr="[MANY_CLASSES_IN_SUPERTYPE_LIST] Only one class may appear in a supertype list"><error descr="[SUPERTYPE_APPEARS_TWICE] A supertype appears twice">bar</error></error> {
}

trait Foo2 : <warning descr="[TRAIT_WITH_SUPERCLASS] Specifying a required base class for trait implementations is deprecated">bar</warning>, Foo {
}

open class Foo1() : bar(), <error>bar</error>, Foo, <error>Foo</error><error>()</error> {}
open class Foo12 : bar(), <error>bar</error> {}