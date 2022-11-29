// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
package test

class Foo

abstract class Base<T> {
    val noGeneric: Foo? = null

    val withOuterGeneric: T? = null

    val <TT> TT.withOwnGeneric: TT? get() = null

    val <TT> TT.withOuterAndOwnGeneric: T? get() = null
}

class <caret>ClassWithGenericBase : Base<Foo>()