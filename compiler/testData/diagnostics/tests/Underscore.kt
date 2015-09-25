// !DIAGNOSTICS: -DEPRECATED_SYMBOL_WITH_MESSAGE

<!UNDESCORE_IS_DEPRECATED!>import kotlin.Deprecated as ___<!>

@___("") data class Pair(val x: Int, val y: Int)

class <!UNDESCORE_IS_DEPRECATED!>_<!><<!UNDESCORE_IS_DEPRECATED!>________<!>>
val <!UNDESCORE_IS_DEPRECATED!>______<!> = _<Int>()

fun <!UNDESCORE_IS_DEPRECATED!>__<!>(<!UNDESCORE_IS_DEPRECATED!>___<!>: Int, y: _<Int>?): Int {
    val (x, <!UNDESCORE_IS_DEPRECATED!>__________<!>) = Pair(___ - 1, 42)
    val <!UNDESCORE_IS_DEPRECATED!>____<!> = x
    // in backquotes: allowed
    val `_` = __________
    <!UNDESCORE_IS_DEPRECATED!>__<!>@ return if (y != null) __(____, y) else __(`_`, ______)
}