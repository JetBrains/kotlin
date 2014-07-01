import kotlin.reflect.KExtensionProperty
import kotlin.test.assertEquals

fun check(expected: String, p: KExtensionProperty<*, *>) {
    var s = p.toString()
    // Strip "val" or "var"
    s = s.substring(4)
    // Strip property name, leave only receiver class
    s = s.substring(0, s.lastIndexOf('.'))

    assertEquals(expected, s)
}

val Boolean.x: Any get() = this
val Char.x: Any get() = this
val Byte.x: Any get() = this
val Short.x: Any get() = this
val Int.x: Any get() = this
val Float.x: Any get() = this
val Long.x: Any get() = this
val Double.x: Any get() = this

val BooleanArray.x: Any get() = this
val CharArray.x: Any get() = this
val ByteArray.x: Any get() = this
val ShortArray.x: Any get() = this
val IntArray.x: Any get() = this
val FloatArray.x: Any get() = this
val LongArray.x: Any get() = this
val DoubleArray.x: Any get() = this

val Array<Int>.a1: Any get() = this
val Array<Any>.a2: Any get() = this
val Array<Array<String>>.a3: Any get() = this
val Array<BooleanArray>.a4: Any get() = this

val Map<String, Runnable>.m: Any get() = this

fun box(): String {
    check("kotlin.Boolean", Boolean::x)
    check("kotlin.Char", Char::x)
    check("kotlin.Byte", Byte::x)
    check("kotlin.Short", Short::x)
    check("kotlin.Int", Int::x)
    check("kotlin.Float", Float::x)
    check("kotlin.Long", Long::x)
    check("kotlin.Double", Double::x)

    check("kotlin.BooleanArray", BooleanArray::x)
    check("kotlin.CharArray", CharArray::x)
    check("kotlin.ByteArray", ByteArray::x)
    check("kotlin.ShortArray", ShortArray::x)
    check("kotlin.IntArray", IntArray::x)
    check("kotlin.FloatArray", FloatArray::x)
    check("kotlin.LongArray", LongArray::x)
    check("kotlin.DoubleArray", DoubleArray::x)

    check("kotlin.Array<java.lang.Integer>", Array<Int>::a1)
    check("kotlin.Array<java.lang.Object>", Array<Any>::a2)
    check("kotlin.Array<kotlin.Array<java.lang.String>>", Array<Array<String>>::a3)
    check("kotlin.Array<kotlin.BooleanArray>", Array<BooleanArray>::a4)

    check("java.util.Map", Map<String, Runnable>::m)

    return "OK"
}
