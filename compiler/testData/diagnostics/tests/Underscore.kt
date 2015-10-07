// !DIAGNOSTICS: -DEPRECATION

<!UNDERSCORE_IS_DEPRECATED!>import kotlin.Deprecated as ___<!>

@___("") data class Pair(val x: Int, val y: Int)

class <!UNDERSCORE_IS_DEPRECATED!>_<!><<!UNDERSCORE_IS_DEPRECATED!>________<!>>
val <!UNDERSCORE_IS_DEPRECATED!>______<!> = _<Int>()

fun <!UNDERSCORE_IS_DEPRECATED!>__<!>(<!UNDERSCORE_IS_DEPRECATED!>___<!>: Int, y: _<Int>?): Int {
    val (x, <!UNDERSCORE_IS_DEPRECATED!>__________<!>) = Pair(___ - 1, 42)
    val <!UNDERSCORE_IS_DEPRECATED!>____<!> = x
    // in backquotes: allowed
    val `_` = __________
    <!UNDERSCORE_IS_DEPRECATED!>__<!>@ return if (y != null) __(____, y) else __(`_`, ______)
}
