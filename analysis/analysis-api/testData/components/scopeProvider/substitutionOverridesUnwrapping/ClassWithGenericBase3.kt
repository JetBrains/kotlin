package test

class SomeClass1
class SomeClass2

interface InterfaceWithFunBase<T1, T2> {
    fun noGenerics_InterfaceWithFunBase() {}

    fun withOuterGenericT1_InterfaceWithFunBase(): T1 {}

    fun withOuterGenericT2_InterfaceWithFunBase(): T2 {}
}

interface InterfaceWithFun<T> : InterfaceWithFunBase<SomeClass1, T> {
    fun noGenerics_InterfaceWithFun() {}

    fun withOuterGeneric_InterfaceWithFun(): T {}
}

abstract class <caret>ClassWithInterfaceWithFun : InterfaceWithFun<SomeClass2>
