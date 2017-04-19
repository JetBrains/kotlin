package test

class SimpleImpl
impl typealias Simple = SimpleImpl
impl fun createSimple(): Simple = SimpleImpl()

class GenericImpl<A, B>
impl typealias Generic<A, B> = GenericImpl<A, B>
impl fun <A, B> createGeneric(a: A, b: B): Generic<A, B> = GenericImpl()
