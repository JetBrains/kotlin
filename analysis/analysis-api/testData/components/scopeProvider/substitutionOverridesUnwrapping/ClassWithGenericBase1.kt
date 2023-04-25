// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
package test

class Foo

abstract class Base<T> {
    fun noGeneric() {}
    
    fun withOuterGeneric(t: T) {}
    
    fun <TT> withOwnGeneric(tt: TT) {}
    
    fun <TT> withOuterAndOwnGeneric(t: T, tt: TT) {}
}

class <caret>ClassWithGenericBase : Base<Foo>()
