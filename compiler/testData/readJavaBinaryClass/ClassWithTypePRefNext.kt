package test

trait Foo<erased Q> : java.lang.Object

open class ClassWithTypePRefNext<erased R : Foo<P>?, erased P>() : java.lang.Object()
