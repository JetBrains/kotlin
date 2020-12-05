// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

class Num<Tn : Number>(val x: Tn)
typealias N<T> = Num<T>

val test0 = N(1)
val test1 = N("1")


class Cons<T>(val head: T, val tail: Cons<T>?)
typealias C<T> = Cons<T>
typealias CC<T> = C<C<T>>

val test2 = <!INAPPLICABLE_CANDIDATE!>C<!>(1, 2)
val test3 = <!INAPPLICABLE_CANDIDATE!>CC<!>(1, 2)
val test4 = CC(C(1, null), null)


class Pair<X, Y>(val x: X, val y: Y)
typealias PL<T> = Pair<T, List<T>>
typealias PN<T> = Pair<T, Num<T>>

val test5 = <!INAPPLICABLE_CANDIDATE!>PL<!>(1, null)


class Foo<T>(val p: Pair<T, T>)
typealias F<T> = Foo<T>

fun testProjections1(x: Pair<in Int, out String>) = <!INAPPLICABLE_CANDIDATE!>F<!>(x)
fun testProjections2(x: Pair<in Int, out Number>) = <!INAPPLICABLE_CANDIDATE!>F<!>(x)
fun testProjections3(x: Pair<in Number, out Int>) = <!INAPPLICABLE_CANDIDATE!>F<!>(x)
fun testProjections4(x: Pair<in Int, in Int>) = <!INAPPLICABLE_CANDIDATE!>F<!>(x)
