trait A<in T> {}
trait B<T> : A<Int> {}
trait C<T> : <error descr="[INCONSISTENT_TYPE_PARAMETER_VALUES] Type parameter T of 'A' has inconsistent values: kotlin.Int, T">B<T>, A<T></error> {}
trait C1<T> : <error descr="[INCONSISTENT_TYPE_PARAMETER_VALUES] Type parameter T of 'A' has inconsistent values: kotlin.Int, kotlin.Any">B<T>, A<Any></error> {}
trait D : <error descr="[INCONSISTENT_TYPE_PARAMETER_VALUES] Type parameter T of 'A' has inconsistent values: kotlin.Int, kotlin.Boolean"><error descr="[INCONSISTENT_TYPE_PARAMETER_VALUES] Type parameter T of 'B' has inconsistent values: kotlin.Boolean, kotlin.Double">C<Boolean>, B<Double></error></error>{}

trait A1<out T> {}
trait B1 : A1<Int> {}
trait B2 : <error descr="[INCONSISTENT_TYPE_PARAMETER_VALUES] Type parameter T of 'A1' has inconsistent values: kotlin.Any, kotlin.Int">A1<Any>, B1</error> {}

trait BA1<T> {}
trait BB1 : BA1<Int> {}
trait BB2 : <error descr="[INCONSISTENT_TYPE_PARAMETER_VALUES] Type parameter T of 'BA1' has inconsistent values: kotlin.Any, kotlin.Int">BA1<Any>, BB1</error> {}


//package x {
    trait xAA1<out T> {}
    trait xAB1 : xAA1<Int> {}
    trait xAB3 : xAA1<Comparable<Int>> {}
    trait xAB2 : <error descr="[INCONSISTENT_TYPE_PARAMETER_VALUES] Type parameter T of 'xAA1' has inconsistent values: kotlin.Number, kotlin.Int, kotlin.Comparable<kotlin.Int>">xAA1<Number>, xAB1, xAB3</error> {}
//}

//package x2 {
    trait x2AA1<out T> {}
    trait x2AB1 : x2AA1<Any> {}
    trait x2AB3 : x2AA1<Comparable<Int>> {}
    trait x2AB2 : <error descr="[INCONSISTENT_TYPE_PARAMETER_VALUES] Type parameter T of 'x2AA1' has inconsistent values: kotlin.Number, kotlin.Any, kotlin.Comparable<kotlin.Int>">x2AA1<Number>, x2AB1, x2AB3</error> {}
//}

//package x3 {
    trait x3AA1<in T> {}
    trait x3AB1 : x3AA1<Any> {}
    trait x3AB3 : x3AA1<Comparable<Int>> {}
    trait x3AB2 : <error descr="[INCONSISTENT_TYPE_PARAMETER_VALUES] Type parameter T of 'x3AA1' has inconsistent values: kotlin.Number, kotlin.Any, kotlin.Comparable<kotlin.Int>">x3AA1<Number>, x3AB1, x3AB3</error> {}
//}

//package sx2 {
    trait sx2AA1<in T> {}
    trait sx2AB1 : sx2AA1<Int> {}
    trait sx2AB3 : sx2AA1<Comparable<Int>> {}
    trait sx2AB2 : <error descr="[INCONSISTENT_TYPE_PARAMETER_VALUES] Type parameter T of 'sx2AA1' has inconsistent values: kotlin.Number, kotlin.Int, kotlin.Comparable<kotlin.Int>">sx2AA1<Number>, sx2AB1, sx2AB3</error> {}
//}