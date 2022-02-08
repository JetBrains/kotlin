// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_VARIABLE, -UNUSED_PARAMETER

fun <T, R> foo(x: T): R = TODO()

fun <T> fooReturnInt(x: T): Int = 1
fun <T> fooTakeString(x: String): T = TODO()

fun <T, R> bar(x: T, y: R, f: (T) -> R): Pair<T, R> = TODO()
fun <T, R> baz(f: (T) -> R, g: (T) -> R): Pair<T, R> = TODO()

class Pair<A, B>(val a: A, val b: B)

fun test1() {
    bar("", 1, ::foo).checkType { _<Pair<String, Int>>() }
    bar("", 1, ::fooReturnInt).checkType { _<Pair<String, Int>>() }
    bar("", 1, ::fooTakeString).checkType { _<Pair<String, Int>>() }
    bar("", "", ::fooReturnInt).checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Pair<String, Any>>() }

    val x: String = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>bar("", "", ::fooReturnInt)<!>

    baz(Int::toString, ::foo).checkType { _<Pair<Int, String>>() }
}

fun <T> listOf(): List<T> = TODO()
fun <T> setOf(): Set<T> = TODO()

fun <T> test2(x: T) {
    bar(x, x, ::foo).checkType { _<Pair<T, T>>() }
    bar(x, 1, ::foo).checkType { _<Pair<T, Int>>() }
    bar(1, x, ::foo).checkType { _<Pair<Int, T>>() }

    bar(listOf<T>(), setOf<T>(), ::foo).checkType { _<Pair<List<T>, Set<T>>> () }
    bar(listOf<T>(), 1, ::foo).checkType { _<Pair<List<T>, Int>>() }
    bar(1, listOf<T>(), ::foo).checkType { _<Pair<Int, List<T>>>() }
}
