// DO_NOT_CHECK_SYMBOL_RESTORE
package test


interface OtherInterface

interface TwoParams<T, TT>

interface MyInterface<T> {
    fun <TT1 : T, TT2 : OtherInterface> funWithOuterAndOwnGenericsAndBounds(tT1: TT1?, tT2: TT2?)

    val <TT1 : T, TT2 : OtherInterface> TwoParams<TT1, TT2>.propWithOuterAndOwnGenericsAndBounds: T? get() = null
}

class Foo

abstract class MyClass : MyInterface<Foo>

// class: test/MyClass