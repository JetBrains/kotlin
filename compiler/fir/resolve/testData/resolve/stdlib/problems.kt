val sb = StringBuilder()
val o = object : Any() {
    val name = "123"

    fun test() {
        name
    }
}
fun test() {
    class Local
    Local()
}
val Any.bar get() = "456"
val String.bar get() = "987"

val t = "".bar

val p = Pair(0, "")

class Base<T>(val x: T)
class Derived : Base<Int>(10)
val xx = Derived().x + 1

val t = throw AssertionError("")

interface A
interface B
interface C : A
class BC : B, C
fun C.analyze() {}
inline fun <reified T> T.analyze() where T : A, T : B {}
fun testAnalyze() {
    BC().analyze()
}
