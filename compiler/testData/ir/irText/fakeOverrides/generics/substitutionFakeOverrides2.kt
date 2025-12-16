// FIR_IDENTICAL

interface A1 : I1<String>, J1
interface I1<T> { fun foo(a: T) }
interface J1 { fun foo(a: String) }

interface A2 : I2<String>, J2
interface I2<T> { fun foo(a: T); fun foo(a: String) }
interface J2 {}

interface A3 : I3<String>, J3
interface I3<T> { fun foo(a: T); fun foo(a: String) }
interface J3 { fun foo(unrelated: Int) {} }
