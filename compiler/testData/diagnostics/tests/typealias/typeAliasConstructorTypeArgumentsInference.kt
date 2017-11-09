class Num<Tn : Number>(val x: Tn)
typealias N<T> = Num<T>

val test0 = N(1)
val test1 = <!TYPE_INFERENCE_UPPER_BOUND_VIOLATED(Type parameter bound for Tn in type inferred from type alias expansion for fun <T> <init>\(x: T\): N<T> /* = Num<T> */
 is not satisfied: inferred type String is not a subtype of Number)!>N<!>("1")


class Cons<T>(val head: T, val tail: Cons<T>?)
typealias C<T> = Cons<T>
typealias CC<T> = C<C<T>>

val test2 = <!TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR!>C<!>(1, <!CONSTANT_EXPECTED_TYPE_MISMATCH!>2<!>)
val test3 = <!TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR!>CC<!>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>, <!CONSTANT_EXPECTED_TYPE_MISMATCH!>2<!>)
val test4 = CC(C(1, null), null)


class Pair<X, Y>(val x: X, val y: Y)
typealias PL<T> = Pair<T, List<T>>
typealias PN<T> = Pair<T, Num<T>>

val test5 = <!TYPE_INFERENCE_INCORPORATION_ERROR!>PL<!>(1, <!NULL_FOR_NONNULL_TYPE!>null<!>)


class Foo<T>(val p: Pair<T, T>)
typealias F<T> = Foo<T>

fun testProjections1(x: Pair<in Int, out String>) = <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>F<!>(x)
fun testProjections2(x: Pair<in Int, out Number>) = <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>F<!>(x)
fun testProjections3(x: Pair<in Number, out Int>) = <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>F<!>(x)
fun testProjections4(x: Pair<in Int, in Int>) = <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>F<!>(x)