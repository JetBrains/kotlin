package test

class SimpleImpl
actual typealias Simple = SimpleImpl
actual fun createSimple(): Simple = SimpleImpl()

class GenericImpl<A, B>
actual typealias Generic<A, B> = GenericImpl<A, B>
actual fun <A, B> createGeneric(a: A, b: B): Generic<A, B> = GenericImpl()
