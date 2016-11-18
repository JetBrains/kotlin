// !DIAGNOSTICS: -DEPRECATION -TOPLEVEL_TYPEALIASES_ONLY

<!UNDERSCORE_IS_RESERVED!>import kotlin.Deprecated as ___<!>

@___("") data class Pair(val x: Int, val y: Int)

class <!UNDERSCORE_IS_RESERVED!>_<!><<!UNDERSCORE_IS_RESERVED!>________<!>>
val <!UNDERSCORE_IS_RESERVED!>______<!> = _<Int>()

fun <!UNDERSCORE_IS_RESERVED!>__<!>(<!UNDERSCORE_IS_RESERVED!>___<!>: Int, y: _<Int>?): Int {
    val (_, <!UNUSED_VARIABLE!>z<!>) = Pair(___ - 1, 42)
    val (x, <!UNDERSCORE_IS_RESERVED!>__________<!>) = Pair(___ - 1, 42)
    val <!UNDERSCORE_IS_RESERVED!>____<!> = x
    // in backquotes: allowed
    val `_` = __________

    val q = fun(_: Int, <!UNDERSCORE_IS_RESERVED, UNUSED_PARAMETER!>__<!>: Int) {}
    q(1, 2)

    val <!UNDERSCORE_IS_RESERVED!>_<!> = 56

    fun localFun(<!UNDERSCORE_IS_RESERVED!>_<!>: String) = 1

    <!UNDERSCORE_IS_RESERVED!>__<!>@ return if (y != null) __(____, y) else __(`_`, ______)
}


class A1(val <!UNDERSCORE_IS_RESERVED, UNDERSCORE_IS_RESERVED!>_<!>: String)
class A2(<!UNDERSCORE_IS_RESERVED!>_<!>: String) {
    class B {
        typealias <!UNDERSCORE_IS_RESERVED!>_<!> = CharSequence
    }
    val <!UNDERSCORE_IS_RESERVED!>_<!>: Int = 1

    fun <!UNDERSCORE_IS_RESERVED!>_<!>() {}

    fun foo(<!UNDERSCORE_IS_RESERVED!>_<!>: Double) {}
}

// one underscore parameters for named function are still prohibited
fun oneUnderscore(<!UNDERSCORE_IS_RESERVED!>_<!>: Int) {}

fun doIt(f: (Any?) -> Any?) = f(null)

val something = doIt { <!UNDERSCORE_IS_RESERVED!>__<!> -> __ }
val something2 = doIt { _ -> 1 }
