// KT-389 Wrong type inference for varargs etc.

import java.util.*

fun foob(vararg a : Byte) : ByteArray = a
fun fooc(vararg a : Char) : CharArray = a
fun foos(vararg a : Short) : ShortArray = a
fun fooi(vararg a : Int) : IntArray = a
fun fool(vararg a : Long) : LongArray = a
fun food(vararg a : Double) : DoubleArray = a
fun foof(vararg a : Float) : FloatArray = a
fun foob(vararg a : Boolean) : BooleanArray = a
fun foos(vararg a : String) : Array<out String> = a

fun test() {
    Arrays.asList(1, 2, 3)
    Arrays.asList<Int>(1, 2, 3)

    foob(1, 2, 3)
    foos(1, 2, 3)
    fooc('1', '2', '3')
    fooi(1, 2, 3)
    fool(1, 2, 3)
    food(1.0, 2.0, 3.0)
    foof(1.0.toFloat(), 2.0.toFloat(), 3.0.toFloat())
}
