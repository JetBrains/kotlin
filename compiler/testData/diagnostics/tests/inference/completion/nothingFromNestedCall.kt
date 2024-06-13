// CHECK_TYPE
// DIAGNOSTICS: -UNUSED_PARAMETER -UNREACHABLE_CODE

class Inv<T>
class Out<out T>

fun <K> invOut(y: K?): Inv<Out<K>> = TODO()
fun <R> test(x: Inv<Out<R>>): R = TODO()

fun testNothing() {
    <!IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION!>test<!>(invOut(null)) checkType { _<Nothing>() }
}
