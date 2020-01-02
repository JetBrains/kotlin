// !DIAGNOSTICS: -UNUSED_VARIABLE

class A<T> {
    class Nested<N>

    inner class Inner<I>
}

val a1 = A::class
val a2 = A<*>::class
val a3 = A<String>::class
val a4 = A<out String?>::class

val n1 = A.Nested::class
val n2 = A.Nested<*>::class

val i1 = A.Inner::class
val i2 = A<*>.Inner<*>::class
val i3 = A<Int>.Inner<CharSequence>::class

val m1 = Map::class
val m2 = Map<Int, *>::class
val m3 = Map.Entry::class

val b1 = Int::class
val b2 = Nothing::class