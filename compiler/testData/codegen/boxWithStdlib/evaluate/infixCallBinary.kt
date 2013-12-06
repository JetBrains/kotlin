import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

Retention(RetentionPolicy.RUNTIME)
annotation class Ann(
        val p1: Int,
        val p2: Int,
        val p3: Int,
        val p4: Int,
        val p5: Int
)

Ann(1 plus 1, 1 minus 1, 1 times 1, 1 div 1, 1 mod 1) class MyClass

fun box(): String {
    val annotation = javaClass<MyClass>().getAnnotation(javaClass<Ann>())!!
    if (annotation.p1 != 2) return "fail 1"
    if (annotation.p2 != 0) return "fail 2"
    if (annotation.p3 != 1) return "fail 3"
    if (annotation.p4 != 1) return "fail 4"
    if (annotation.p5 != 0) return "fail 5"
    return "OK"
}