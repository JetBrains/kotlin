// !DIAGNOSTICS: -UNUSED_VARIABLE

class A<T> {
    class Nested<N>

    inner class Inner<I>
}

val a1 = A::class
val a2 = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>A<*>::class<!>
val a3 = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>A<String>::class<!>
val a4 = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>A<out String?>::class<!>

val n1 = A.Nested::class
val n2 = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>A.Nested<*>::class<!>

val i1 = A.Inner::class
val i2 = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>A<*>.Inner<*>::class<!>
val i3 = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>A<<!DEBUG_INFO_MISSING_UNRESOLVED!>Int<!>>.Inner<CharSequence>::class<!>

val m1 = Map::class
val m2 = <!CLASS_LITERAL_LHS_NOT_A_CLASS!>Map<Int, *>::class<!>
val m3 = Map.Entry::class

val b1 = Int::class
val b2 = Nothing::class
