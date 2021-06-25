// FIR_IDENTICAL
// FILE: a.kt
interface A<in T> {}
interface B<T> : A<Int> {}
interface C<T> : <!INCONSISTENT_TYPE_PARAMETER_VALUES!>B<T>, A<T><!> {}
interface C1<T> : <!INCONSISTENT_TYPE_PARAMETER_VALUES!>B<T>, A<Any><!> {}
interface D : <!INCONSISTENT_TYPE_PARAMETER_VALUES, INCONSISTENT_TYPE_PARAMETER_VALUES!>C<Boolean>, B<Double><!>{}

interface A1<out T> {}
interface B1 : A1<Int> {}
interface B2 : <!INCONSISTENT_TYPE_PARAMETER_VALUES!>A1<Any>, B1<!> {}

interface BA1<T> {}
interface BB1 : BA1<Int> {}
interface BB2 : <!INCONSISTENT_TYPE_PARAMETER_VALUES!>BA1<Any>, BB1<!> {}


// FILE: b.kt
package x
interface AA1<out T> {}
interface AB1 : AA1<Int> {}
interface AB3 : AA1<Comparable<Int>> {}
interface AB2 : <!INCONSISTENT_TYPE_PARAMETER_VALUES!>AA1<Number>, AB1, AB3<!> {}

// FILE: c.kt
package x2
interface AA1<out T> {}
interface AB1 : AA1<Any> {}
interface AB3 : AA1<Comparable<Int>> {}
interface AB2 : <!INCONSISTENT_TYPE_PARAMETER_VALUES!>AA1<Number>, AB1, AB3<!> {}

// FILE: d.kt
package x3
interface AA1<in T> {}
interface AB1 : AA1<Any> {}
interface AB3 : AA1<Comparable<Int>> {}
interface AB2 : <!INCONSISTENT_TYPE_PARAMETER_VALUES!>AA1<Number>, AB1, AB3<!> {}

// FILE: e.kt
package sx2
interface AA1<in T> {}
interface AB1 : AA1<Int> {}
interface AB3 : AA1<Comparable<Int>> {}
interface AB2 : <!INCONSISTENT_TYPE_PARAMETER_VALUES!>AA1<Number>, AB1, AB3<!> {}

// FILE: f.kt
interface I0<T1, T2>
abstract class C2<T3, T4> : I0<T3, T4>
typealias TA<T5, T6> = C2<T6, T5>
interface I2
interface I3
class C3 : TA<I2, I3>(), I0<I3, I2>