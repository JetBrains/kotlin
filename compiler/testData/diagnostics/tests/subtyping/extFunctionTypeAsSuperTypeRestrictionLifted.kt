// FIR_IDENTICAL
// !LANGUAGE: +FunctionalTypeWithExtensionAsSupertype
// SKIP_TXT
class A : Double.() -> Unit {
    override fun invoke(p1: Double) {}
}

class B : Int.(Double) -> Unit {
    override fun invoke(p1: Int, p2: Double) {}
}

open class C {}

abstract class A0 : C.() -> Int
abstract class A1 : C.(Int) -> Int
abstract class A2 : C.(Int, String) -> Int

open class D<T> {}

abstract class B0<T> : D<T>.() -> Int
abstract class B1<T> : D<T>.(C) -> Int
abstract class B2<T> : D<T>.(T, C) -> T

interface E<T> {}

abstract class C0: C(), Int.() -> Double
abstract class C1<T>: C(), E<T>, Int.(C) -> Double
