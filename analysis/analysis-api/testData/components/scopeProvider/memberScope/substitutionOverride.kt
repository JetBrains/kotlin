// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
// class: test/ClassWithGenericBase
package test

class Foo

abstract class Base<T> {
    fun noGeneric() = 42
    fun noGenericWithExplicitType(): Int = 24
    fun withOuterGeneric(t: T) = "str"
    fun withOuterGenericWithExplicitType(t: T): String = "rts"
    fun <TT> withOwnGeneric(tt: TT) = true
    fun <TT> withOwnGenericWithExplicitType(tt: TT): Boolean = false
    fun <TT> withOuterAndOwnGeneric(t: T, tt: TT) = 4L
    fun <TT> withOuterAndOwnGenericWithExplicitType(t: T, tt: TT): Long = 1L
}

class ClassWithGenericBase : Base<Foo>()
