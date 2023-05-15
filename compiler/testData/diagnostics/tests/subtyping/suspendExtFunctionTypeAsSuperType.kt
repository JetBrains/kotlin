class A : <!SUPERTYPE_IS_SUSPEND_EXTENSION_FUNCTION_TYPE!>suspend Double.() -> Unit<!> {
    override suspend fun invoke(p1: Double) {}
}

class B : <!SUPERTYPE_IS_SUSPEND_EXTENSION_FUNCTION_TYPE!>suspend Int.(Double) -> Unit<!> {
    override suspend fun invoke(p1: Int, p2: Double) {}
}

open class C {}

abstract class A0 : <!SUPERTYPE_IS_SUSPEND_EXTENSION_FUNCTION_TYPE!>suspend C.() -> Int<!>
abstract class A1 : <!SUPERTYPE_IS_SUSPEND_EXTENSION_FUNCTION_TYPE!>suspend C.(Int) -> Int<!>
abstract class A2 : <!SUPERTYPE_IS_SUSPEND_EXTENSION_FUNCTION_TYPE!>suspend C.(Int, String) -> Int<!>

open class D<T> {}

abstract class B0<T> : <!SUPERTYPE_IS_SUSPEND_EXTENSION_FUNCTION_TYPE!>suspend D<T>.() -> Int<!>
abstract class B1<T> : <!SUPERTYPE_IS_SUSPEND_EXTENSION_FUNCTION_TYPE!>suspend D<T>.(C) -> Int<!>
abstract class B2<T> : <!SUPERTYPE_IS_SUSPEND_EXTENSION_FUNCTION_TYPE!>suspend D<T>.(T, C) -> T<!>

interface E<T> {}

abstract class C0: C(), <!SUPERTYPE_IS_SUSPEND_EXTENSION_FUNCTION_TYPE!>suspend Int.() -> Double<!>
abstract class C1<T>: C(), E<T>, <!SUPERTYPE_IS_SUSPEND_EXTENSION_FUNCTION_TYPE!>suspend Int.(C) -> Double<!>
