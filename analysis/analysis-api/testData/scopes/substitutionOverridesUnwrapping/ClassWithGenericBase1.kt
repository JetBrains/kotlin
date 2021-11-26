package test

class Foo

abstract class Base<T> {
    fun noGeneric() {}
    
    fun withOuterGeneric(t: T) {}
    
    fun <TT> withOwnGeneric(tt: TT) {}
    
    fun <TT> withOuterAndOwnGeneric(t: T, tt: TT) {}
}

class <caret>ClassWithGenericBase : Base<Foo>()
