// !DIAGNOSTICS: -REDUNDANT_PROJECTION -CONFLICTING_PROJECTION

interface G

val <T> T.a: Int
    get() = 3

val <T1, T2> Map<T1, T2>.b: String
    get() = "asds"

val <T : G> G.c: Int get() = 5

val <T1, T2, T3> List<Map<T2, T3>>.d: Int get() = 6

val <T: Any> G.e: T?
    get() = null

val <T> List<Map<Int, Map<String, T>>>.f: Int get() = 7

val <T> List<Map<Int, Map<String, out T>>>.g: Int get() = 7
val <T> List<Map<Int, Map<String, in T>>>.h: Int get() = 7

val <T> List<Map<T, Map<T, T>>>.i: Int get() = 7

var <T1, T2, T3, T4> p = 1

class C<T1, T2> {
    val <E> T1.a: Int get() = 3
    val <E> T2.b: Int get() = 3
    val <E> E.c: Int get() = 3
    val <E> Map<T1, T2>.d: Int get() = 3
    val <E> Map<T1, E>.e: Int get() = 3
}

val <T : Enum<T>> T.z1: Int
    get() = 4

interface D<T : Enum<T>>

val <X: D<*>> X.z2: Int
    get() = 4

val <Y> D<*>.z3: Int
    get() = 4