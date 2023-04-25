// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
package test

class SomeClass1
class SomeClass2

interface InterfaceWithValBase<T1, T2> {
    val noGenerics_InterfaceWithValBase: SomeClass1

    val withOuterGenericT1_InterfaceWithValBase: T1

    val withOuterGenericT2_InterfaceWithValBase: T2

    val <Own> Own.withOwnGeneric_InterfaceWithValBase: SomeClass1

    val <Own> Own.withOwnAndOuterGenericT1_InterfaceWithValBase: T1

    val <Own> Own.withOwnAndOuterGenericT2_InterfaceWithValBase: T2
}

interface InterfaceWithVal<T> : InterfaceWithValBase<SomeClass1, T> {
    val noGenerics_InterfaceWithVal: SomeClass1

    val withOuterGeneric_InterfaceWithVal: T

    val <Own> Own.withOwnGeneric_InterfaceWithVal: SomeClass1

    val <Own> Own.withOwnAndOuterGeneric_InterfaceWithVal: T
}


abstract class <caret>ClassWithInterfaceWithVal : InterfaceWithVal<SomeClass2>
