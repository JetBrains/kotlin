// NI_EXPECTED_FILE

class Num<Tn : Number>(val x: Tn)
typealias N<T> = Num<T>

val test0 = N(1)
val test1 = <!CANNOT_INFER_PARAMETER_TYPE!>N<!>(<!ARGUMENT_TYPE_MISMATCH!>"1"<!>)


class Cons<T>(val head: T, val tail: Cons<T>?)
typealias C<T> = Cons<T>
typealias CC<T> = C<C<T>>

val test2 = C(1, <!ARGUMENT_TYPE_MISMATCH!>2<!>)
val test3 = <!CANNOT_INFER_PARAMETER_TYPE!>CC<!>(<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!ARGUMENT_TYPE_MISMATCH!>2<!>)
val test4 = CC(C(1, null), null)


class Pair<X, Y>(val x: X, val y: Y)
typealias PL<T> = Pair<T, List<T>>
typealias PN<T> = Pair<T, Num<T>>

val test5 = PL(1, <!NULL_FOR_NONNULL_TYPE!>null<!>)


class Foo<T>(val p: Pair<T, T>)
typealias F<T> = Foo<T>

fun testProjections1(x: Pair<in Int, out String>) = <!CANNOT_INFER_PARAMETER_TYPE!>F<!>(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
fun testProjections2(x: Pair<in Int, out Number>) = <!CANNOT_INFER_PARAMETER_TYPE!>F<!>(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
fun testProjections3(x: Pair<in Number, out Int>) = <!CANNOT_INFER_PARAMETER_TYPE!>F<!>(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
fun testProjections4(x: Pair<in Int, in Int>) = <!CANNOT_INFER_PARAMETER_TYPE!>F<!>(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
