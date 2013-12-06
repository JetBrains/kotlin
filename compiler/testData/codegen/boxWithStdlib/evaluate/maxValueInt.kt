import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

Retention(RetentionPolicy.RUNTIME)
annotation class Ann(
        val p1: Int,
        val p2: Int,
        val p4: Long,
        val p5: Int
)

Ann(
    p1 = java.lang.Integer.MAX_VALUE + 1,
    p2 = 1 + 1,
    p4 = 1 + 1,
    p5 = 1.toInt() + 1.toInt()
) class MyClass

fun box(): String {
    val annotation = javaClass<MyClass>().getAnnotation(javaClass<Ann>())!!
    if (annotation.p1 != -2147483648) return "fail 1, expected = ${-2147483648}, actual = ${annotation.p1}"
    if (annotation.p2 != 2) return "fail 2, expected = ${2}, actual = ${annotation.p2}"
    if (annotation.p4 != 2.toLong()) return "fail 4, expected = ${2}, actual = ${annotation.p4}"
    if (annotation.p5 != 2) return "fail 5, expected = ${2}, actual = ${annotation.p5}"
    return "OK"
}