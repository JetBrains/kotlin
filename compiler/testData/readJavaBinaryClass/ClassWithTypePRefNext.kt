package test

trait Foo<Q> : java.lang.Object

open class ClassWithTypePRefNext<R : Foo<P>?, P>() : java.lang.Object()
