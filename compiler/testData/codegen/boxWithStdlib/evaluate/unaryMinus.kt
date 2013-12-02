import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

Retention(RetentionPolicy.RUNTIME)
annotation class Ann(
        val b1: Byte,
        val b2: Short,
        val b3: Int,
        val b4: Long,
        val b5: Double,
        val b6: Float
)

val b1: Byte = -1
val b2: Short = -1
val b3: Int = -1
val b4: Long = -1
val b5: Double = -1.0
val b6: Float = -1.0.toFloat()

Ann(b1, b2, b3, b4, b5, b6) class MyClass

fun box(): String {
    val annotation = javaClass<MyClass>().getAnnotation(javaClass<Ann>())!!
    if (annotation.b1 != b1) return "fail 1"
    if (annotation.b2 != b2) return "fail 2"
    if (annotation.b3 != b3) return "fail 3"
    if (annotation.b4 != b4) return "fail 4"
    if (annotation.b5 != b5) return "fail 5"
    if (annotation.b6 != b6) return "fail 6"
    return "OK"
}