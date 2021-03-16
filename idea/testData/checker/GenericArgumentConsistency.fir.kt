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


//package x {
    interface xAA1<out T> {}
    interface xAB1 : xAA1<Int> {}
    interface xAB3 : xAA1<Comparable<Int>> {}
    interface xAB2 : xAA1<Number>, xAB1, xAB3 {}
//}

//package x2 {
    interface x2AA1<out T> {}
    interface x2AB1 : x2AA1<Any> {}
    interface x2AB3 : x2AA1<Comparable<Int>> {}
    interface x2AB2 : x2AA1<Number>, x2AB1, x2AB3 {}
//}

//package x3 {
    interface x3AA1<in T> {}
    interface x3AB1 : x3AA1<Any> {}
    interface x3AB3 : x3AA1<Comparable<Int>> {}
    interface x3AB2 : x3AA1<Number>, x3AB1, x3AB3 {}
//}

//package sx2 {
    interface sx2AA1<in T> {}
    interface sx2AB1 : sx2AA1<Int> {}
    interface sx2AB3 : sx2AA1<Comparable<Int>> {}
    interface sx2AB2 : sx2AA1<Number>, sx2AB1, sx2AB3 {}
//}
