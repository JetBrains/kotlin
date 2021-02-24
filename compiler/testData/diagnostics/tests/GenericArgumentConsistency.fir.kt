// FILE: a.kt
interface A<in T> {}
interface B<T> : A<Int> {}
interface C<T> : B<T>, A<T> {}
interface C1<T> : B<T>, A<Any> {}
interface D : C<Boolean>, B<Double>{}

interface A1<out T> {}
interface B1 : A1<Int> {}
interface B2 : A1<Any>, B1 {}

interface BA1<T> {}
interface BB1 : BA1<Int> {}
interface BB2 : BA1<Any>, BB1 {}


// FILE: b.kt
package x
    interface AA1<out T> {}
    interface AB1 : AA1<Int> {}
    interface AB3 : AA1<Comparable<Int>> {}
    interface AB2 : AA1<Number>, AB1, AB3 {}

// FILE: c.kt
package x2
    interface AA1<out T> {}
    interface AB1 : AA1<Any> {}
    interface AB3 : AA1<Comparable<Int>> {}
    interface AB2 : AA1<Number>, AB1, AB3 {}

// FILE: d.kt
package x3
    interface AA1<in T> {}
    interface AB1 : AA1<Any> {}
    interface AB3 : AA1<Comparable<Int>> {}
    interface AB2 : AA1<Number>, AB1, AB3 {}

// FILE: e.kt
package sx2
    interface AA1<in T> {}
    interface AB1 : AA1<Int> {}
    interface AB3 : AA1<Comparable<Int>> {}
    interface AB2 : AA1<Number>, AB1, AB3 {}
