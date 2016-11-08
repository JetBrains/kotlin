class Num<Tn : Number>(val x: Tn)
typealias N<T> = Num<T>

class Cons<T>(val head: T, val tail: Cons<T>?)
typealias C<T> = Cons<T>
typealias CC<T> = C<C<T>>

class Pair<X, Y>(val x: X, val y: Y)
typealias PL<T> = Pair<T, List<T>>

class Bound<X, Y : X>(val x: X, val y: Y)
typealias B<X, Y> = Bound<X, Y>

class Foo<T>(val p: Pair<T, T>)
typealias F<T> = Foo<T>

val test0 = N(1)
val test1 = <!TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>N<!>("1")
val test2 = <!TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR!>C<!>(1, <!CONSTANT_EXPECTED_TYPE_MISMATCH!>2<!>)
val test3 = <!TYPE_INFERENCE_INCORPORATION_ERROR!>PL<!>(1, <!NULL_FOR_NONNULL_TYPE!>null<!>)
val test4 = <!TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR!>CC<!>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>, <!CONSTANT_EXPECTED_TYPE_MISMATCH!>2<!>)
val test4a = CC(C(1, null), null)

fun testProjections1(x: Pair<in Int, out String>) = <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>F<!>(x)
fun testProjections2(x: Pair<in Int, out Number>) = <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>F<!>(x)
fun testProjections3(x: Pair<in Number, out Int>) = <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>F<!>(x)
fun testProjections4(x: Pair<in Int, in Int>) = <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>F<!>(x)