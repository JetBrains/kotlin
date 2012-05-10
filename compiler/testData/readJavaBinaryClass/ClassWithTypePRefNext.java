package test;

interface Foo<Q> {
}

class ClassWithTypePRefNext<R extends Foo<P>, P> {
}
