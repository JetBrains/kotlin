import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import a.*

Ann(i, s, f, d, l, b, bool, c, str)
Ann(i2, s2, f2, d2, l2, b2, bool2, c2, str2)
class MyClass

Retention(RetentionPolicy.RUNTIME)
annotation class Ann(
        val i: Int,
        val s: Short,
        val f: Float,
        val d: Double,
        val l: Long,
        val b: Byte,
        val bool: Boolean,
        val c: Char,
        val str: String
)

fun main(args: Array<String>) {
    MyClass()
}


