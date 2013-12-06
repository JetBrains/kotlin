import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

Retention(RetentionPolicy.RUNTIME)
annotation class Ann(
        val p1: Int,
        val p2: Byte,
        val p4: Int,
        val p5: Int
)

Ann(
        p1 = java.lang.Byte.MAX_VALUE + 1,
        p2 = 1 + 1,
        p4 = 1 + 1,
        p5 = 1.toByte() + 1
) class MyClass

fun box(): String {
    val annotation = javaClass<MyClass>().getAnnotation(javaClass<Ann>())!!
    if (annotation.p1 != 128) return "fail 1, expected = ${128}, actual = ${annotation.p1}"
    if (annotation.p2 != 2.toByte()) return "fail 2, expected = ${2}, actual = ${annotation.p2}"
    if (annotation.p4 != 2) return "fail 4, expected = ${2}, actual = ${annotation.p4}"
    if (annotation.p5 != 2) return "fail 5, expected = ${2}, actual = ${annotation.p5}"
    return "OK"
}