// !DIAGNOSTICS: -DEPRECATION -TOPLEVEL_TYPEALIASES_ONLY

import kotlin.Deprecated as ___

@___("") data class Pair(val x: Int, val y: Int)

class _<________>
val ______ = _<Int>()

fun __(___: Int, y: _<Int>?): Int {
    val (_, z) = Pair(___ - 1, 42)
    val (x, __________) = Pair(___ - 1, 42)
    val ____ = x
    // in backquotes: allowed
    val `_` = __________

    val q = fun(_: Int, __: Int) {}
    q(1, 2)

    val _ = 56

    fun localFun(_: String) = 1

    __@ return if (y != null) __(____, y) else __(`_`, ______)
}


class A1(val _: String)
class A2(_: String) {
    class B {
        typealias _ = CharSequence
    }
    val _: Int = 1

    fun _() {}

    fun foo(_: Double) {}
}

// one underscore parameters for named function are still prohibited
fun oneUnderscore(_: Int) {}

fun doIt(f: (Any?) -> Any?) = f(null)

val something = doIt { __ -> __ }
val something2 = doIt { _ -> 1 }
