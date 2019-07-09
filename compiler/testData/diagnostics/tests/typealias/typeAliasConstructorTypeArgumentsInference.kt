// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

class Num<Tn : Number>(val x: Tn)
typealias N<T> = Num<T>

val test0 = N(1)
val test1 = <!OI;TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>N<!>(<!NI;TYPE_MISMATCH!>"1"<!>)


class Cons<T>(val head: T, val tail: Cons<T>?)
typealias C<T> = Cons<T>
typealias CC<T> = C<C<T>>

val test2 = <!OI;TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR!>C<!>(1, <!CONSTANT_EXPECTED_TYPE_MISMATCH!>2<!>)
val test3 = <!OI;TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR!>CC<!>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>, <!CONSTANT_EXPECTED_TYPE_MISMATCH!>2<!>)
val test4 = CC(C(1, null), null)


class Pair<X, Y>(val x: X, val y: Y)
typealias PL<T> = Pair<T, List<T>>
typealias PN<T> = Pair<T, Num<T>>

val test5 = <!OI;TYPE_INFERENCE_INCORPORATION_ERROR!>PL<!>(1, <!NULL_FOR_NONNULL_TYPE!>null<!>)


class Foo<T>(val p: Pair<T, T>)
typealias F<T> = Foo<T>

fun testProjections1(x: Pair<in Int, out String>) = <!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>F<!>(<!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH, NI;TYPE_MISMATCH!>x<!>)
fun testProjections2(x: Pair<in Int, out Number>) = <!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>F<!>(<!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH, NI;TYPE_MISMATCH!>x<!>)
fun testProjections3(x: Pair<in Number, out Int>) = <!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>F<!>(<!NI;TYPE_MISMATCH!>x<!>)
fun testProjections4(x: Pair<in Int, in Int>) = <!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>F<!>(<!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH, NI;TYPE_MISMATCH!>x<!>)