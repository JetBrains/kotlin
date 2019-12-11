class A : () -> Unit {
    override fun invoke() {}
}

class AA : Function0<Unit> {
    override fun invoke() {}
}

class B : (Double) -> Unit {
    override fun invoke(p1: Double) {}
}

class BB : Function1<Double, Unit> {
    override fun invoke(p1: Double) {}
}

open class C {}

abstract class A0 : (C) -> Int
abstract class A1 : Function1<C, Int>

abstract class A2 : (C, Int) -> Int
abstract class A3 : Function2<C, Int, Int>

abstract class A4 : (Int, C, String) -> Int
abstract class A5 : Function3<Int, C, String, Int>

open class D<T> {}

abstract class B0<T> : (D<T>) -> Int
abstract class B1<T> : Function1<D<T>, Int>

abstract class B2<T> : (D<T>, C) -> Int
abstract class B3<T> : Function2<D<T>, C, Int>

abstract class B4<T> : (D<T>, C) -> T
abstract class B5<T> : Function2<D<T>, C, T>

interface E<T> {}

abstract class C0: C(), (Int) -> Double
abstract class C1: C(), Function1<Int, Double>

abstract class C2<T>: C(), E<T>, (Int, C) -> Double
abstract class C3<T>: C(), E<T>, Function2<Int, C, Double>