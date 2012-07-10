// FILE: b.kt
trait A<in T> {}
trait B<T> : A<Int> {}
trait C<T> : <!INCONSISTENT_TYPE_PARAMETER_VALUES!>B<T>, A<T><!> {}
trait C1<T> : B<T>, A<Any> {}
trait D : <!INCONSISTENT_TYPE_PARAMETER_VALUES, INCONSISTENT_TYPE_PARAMETER_VALUES!>C<Boolean>, B<Double><!>{}

trait A1<out T> {}
trait B1 : A1<Int> {}
trait B2 : A1<Any>, B1 {}

trait BA1<T> {}
trait BB1 : BA1<Int> {}
trait BB2 : <!INCONSISTENT_TYPE_PARAMETER_VALUES!>BA1<Any>, BB1<!> {}


// FILE: b.kt
package x
    trait AA1<out T> {}
    trait AB1 : AA1<Int> {}
    trait AB3 : AA1<Comparable<Int>> {}
    trait AB2 : AA1<Number>, AB1, AB3 {}

// FILE: b.kt
package x2
    trait AA1<out T> {}
    trait AB1 : AA1<Any> {}
    trait AB3 : AA1<Comparable<Int>> {}
    trait AB2 : <!INCONSISTENT_TYPE_PARAMETER_VALUES!>AA1<Number>, AB1, AB3<!> {}

// FILE: b.kt
package x3
    trait AA1<in T> {}
    trait AB1 : AA1<Any> {}
    trait AB3 : AA1<Comparable<Int>> {}
    trait AB2 : AA1<Number>, AB1, AB3 {}

// FILE: b.kt
package sx2
    trait AA1<in T> {}
    trait AB1 : AA1<Int> {}
    trait AB3 : AA1<Comparable<Int>> {}
    trait AB2 : <!INCONSISTENT_TYPE_PARAMETER_VALUES!>AA1<Number>, AB1, AB3<!> {}
