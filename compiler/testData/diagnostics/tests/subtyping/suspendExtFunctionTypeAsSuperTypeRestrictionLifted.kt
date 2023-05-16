// FIR_IDENTICAL
// !LANGUAGE: +FunctionalTypeWithExtensionAsSupertype
class A : suspend Double.() -> Unit {
    override suspend fun invoke(p1: Double) {}
}

class B : suspend Int.(Double) -> Unit {
    override suspend fun invoke(p1: Int, p2: Double) {}
}

open class C {}

abstract class A0 : suspend C.() -> Int
abstract class A1 : suspend C.(Int) -> Int
abstract class A2 : suspend C.(Int, String) -> Int

open class D<T> {}

abstract class B0<T> : suspend D<T>.() -> Int
abstract class B1<T> : suspend D<T>.(C) -> Int
abstract class B2<T> : suspend D<T>.(T, C) -> T

interface E<T> {}

abstract class C0: C(), suspend Int.() -> Double
abstract class C1<T>: C(), E<T>, suspend Int.(C) -> Double
