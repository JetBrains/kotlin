trait A<in T> {}
trait B<T> : A<Int> {}
trait C<T> : <error>B<T>, A<T></error> {}
trait C1<T> : <error>B<T>, A<Any></error> {}
trait D : <error><error>C<Boolean>, B<Double></error></error>{}

trait A1<out T> {}
trait B1 : A1<Int> {}
trait B2 : <error>A1<Any>, B1</error> {}

trait BA1<T> {}
trait BB1 : BA1<Int> {}
trait BB2 : <error>BA1<Any>, BB1</error> {}


//package x {
    trait xAA1<out T> {}
    trait xAB1 : xAA1<Int> {}
    trait xAB3 : xAA1<Comparable<Int>> {}
    trait xAB2 : <error>xAA1<Number>, xAB1, xAB3</error> {}
//}

//package x2 {
    trait x2AA1<out T> {}
    trait x2AB1 : x2AA1<Any> {}
    trait x2AB3 : x2AA1<Comparable<Int>> {}
    trait x2AB2 : <error>x2AA1<Number>, x2AB1, x2AB3</error> {}
//}

//package x3 {
    trait x3AA1<in T> {}
    trait x3AB1 : x3AA1<Any> {}
    trait x3AB3 : x3AA1<Comparable<Int>> {}
    trait x3AB2 : <error>x3AA1<Number>, x3AB1, x3AB3</error> {}
//}

//package sx2 {
    trait sx2AA1<in T> {}
    trait sx2AB1 : sx2AA1<Int> {}
    trait sx2AB3 : sx2AA1<Comparable<Int>> {}
    trait sx2AB2 : <error>sx2AA1<Number>, sx2AB1, sx2AB3</error> {}
//}