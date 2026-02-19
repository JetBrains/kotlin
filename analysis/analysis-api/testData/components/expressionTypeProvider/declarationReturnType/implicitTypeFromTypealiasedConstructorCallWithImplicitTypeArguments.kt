package test

class GenericClass<T>(t: T)

class MyClass

typealias TypeAlias<TT> = GenericClass<TT>

val propertyWithImplicitType = TypeAlias(MyClass())