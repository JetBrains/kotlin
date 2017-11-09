// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

class Pair<T1, T2>(val x1: T1, val x2: T2)

class C<T> {
    typealias P2 = Pair<T, T>
    typealias PT2<T2> = Pair<T, T2>

    fun first(p: P2) = p.x1
    fun second(p: P2) = p.x2

    fun <T2> first2(p: PT2<T2>) = p.x1
    fun <T2> second2(p: PT2<T2>) = p.x2
}

val p1 = Pair(1, 1)
val p2 = Pair(1, "")

val test1: Int = C<Int>().first(p1)
val test2: Int = C<Int>().second(p1)

val test3: Int = C<Int>().first2(p2)
val test4: String = C<Int>().second2(p2)
