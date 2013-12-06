import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

Retention(RetentionPolicy.RUNTIME)
annotation class Ann(
        val b: Byte,
        val s: Short,
        val i: Int,
        val l: Long
)

Ann(1 / 1, 1 / 1, 1 / 1, 1 / 1) class MyClass

fun box(): String {
    val annotation = javaClass<MyClass>().getAnnotation(javaClass<Ann>())!!
    if (annotation.b != 1.toByte()) return "fail 1"
    if (annotation.s != 1.toShort()) return "fail 2"
    if (annotation.i != 1) return "fail 2"
    if (annotation.l != 1.toLong()) return "fail 2"
    return "OK"
}

// EXPECTED: Ann[b = IntegerValueType(1): IntegerValueType(1), i = IntegerValueType(1): IntegerValueType(1), l = IntegerValueType(1): IntegerValueType(1), s = IntegerValueType(1): IntegerValueType(1)]
