// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

fun <T> select(x: T, y: T): T = x

fun foo(x: Int, stringNothing: Out2<String, Nothing>): Out2<String, Int> =
    select(x.right(), stringNothing)

fun <R> R.right(): Out2<Nothing, R> = TODO()

class Out2<out K, out V>